package dev.jbang.idea.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import dev.jbang.idea.JBangCli.generateScriptFromTemplate
import dev.jbang.idea.JBangCli.listJBangTemplates
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project

class CreateFromTemplateAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            val templates = listJBangTemplates()
            val dialogWrapper = JBangTemplatesDialogWrapper(templates)
            if (dialogWrapper.showAndGet()) {
                val scriptName = dialogWrapper.getScriptFileName()
                val templateName = dialogWrapper.getTemplateName()
                if (scriptName.isNotEmpty()) {
                    ApplicationManager.getApplication().runWriteAction {
                        val project = e.getData(CommonDataKeys.PROJECT)!!
                        val directory = e.getData(CommonDataKeys.VIRTUAL_FILE)!!
                        try {
                            val currentFiles = mutableListOf<VirtualFile>()
                            //iterate all current files
                            VfsUtil.iterateChildrenRecursively(
                                directory,
                                VirtualFileFilter.ALL
                            ) {
                                if (!it.isDirectory) {
                                    currentFiles.add(it)
                                }
                                true
                            }
                            // generate script from JBang CLI
                            generateScriptFromTemplate(templateName, scriptName, directory.path)
                            // refresh directory
                            VfsUtil.markDirtyAndRefresh(false, true, true, directory)
                            val fileEditorManager = FileEditorManager.getInstance(project)
                            //iterate all files and open new files
                            VfsUtil.iterateChildrenRecursively(
                                directory,
                                VirtualFileFilter.ALL
                            ) {
                                if (!it.isDirectory && !currentFiles.contains(it)) {
                                    fileEditorManager.openFile(it, true)
                                }
                                true
                            }
                        } catch (e: Exception) {
                            val errorText = "Failed to create script from template, please check template and script name!"
                            val jbangNotificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("JBang Failure")
                            jbangNotificationGroup.createNotification("Failed to resolve DEPS", errorText, NotificationType.ERROR).notify(project)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val errorText = e.message!!
            val jbangNotificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("JBang Failure")
            jbangNotificationGroup.createNotification("Failed to get JBang templates", errorText, NotificationType.ERROR).notify(project)
        }
    }
}