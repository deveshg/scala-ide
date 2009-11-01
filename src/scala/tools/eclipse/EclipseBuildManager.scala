/*
 * Copyright 2005-2009 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse

import org.eclipse.core.resources.{ IFile, IMarker }
import org.eclipse.core.runtime.IProgressMonitor

import scala.tools.nsc.{ Global, Settings }
import scala.tools.nsc.interactive.RefinedBuildManager
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.Position

import scala.tools.eclipse.properties.PropertyStore
import scala.tools.eclipse.util.{ EclipseResource, FileUtils } 

class EclipseBuildManager(project : ScalaProject, settings0: Settings) extends RefinedBuildManager(settings0) {
  var monitor : IProgressMonitor = _
    
  class EclipseBuildCompiler(settings : Settings, reporter : Reporter) extends BuilderGlobal(settings, reporter) {

    def buildReporter = reporter.asInstanceOf[BuildReporter]
    
    buildReporter.compiler = this
    
    override def newRun() =
      new Run {
        var worked = 0
        
        override def progress(current : Int, total : Int) : Unit = {
          if (monitor != null && monitor.isCanceled) {
            cancel
            return
          }
          
          val newWorked = if (current >= total) 100 else ((current.toDouble/total)*100).toInt
          if (worked < newWorked) {
            if (monitor != null)
              monitor.worked(newWorked-worked)
            worked = newWorked
          }
        }
      
        override def compileLate(file : AbstractFile) = {
          file match {
            case EclipseResource(i : IFile) =>
              FileUtils.clearBuildErrors(i, monitor)
              FileUtils.clearTasks(i, monitor)
            case _ => 
          }
          super.compileLate(file)
        }
      }
  }

  class BuildReporter(project : ScalaProject) extends Reporter {
    var compiler : Global = _
    
    val taskScanner = new TaskScanner(project)
    
    override def info0(pos : Position, msg : String, severity : Severity, force : Boolean) = {
      severity.count += 1

      val eclipseSeverity = severity.id match {
        case 2 => IMarker.SEVERITY_ERROR
        case 1 => IMarker.SEVERITY_WARNING
        case 0 => IMarker.SEVERITY_INFO
      }
      
      try {
        if(pos.isDefined) {
          val source = pos.source
          val length = source.identifier(pos, compiler).map(_.length).getOrElse(0)
          source.file match {
            case EclipseResource(i : IFile) => FileUtils.buildError(i, eclipseSeverity, msg, pos.point, length, pos.line, null)
            case _ => project.buildError(eclipseSeverity, msg, null)
          }
        }
        else
          project.buildError(eclipseSeverity, msg, null)
      } catch {
        case ex : UnsupportedOperationException => 
          project.buildError(eclipseSeverity, msg, null)
      }
    }
    
    override def comment(pos : Position, msg : String) {
      val tasks = taskScanner.extractTasks(msg, pos)
      for (TaskScanner.Task(tag, msg, priority, pos) <- tasks if pos.isDefined) {
        val source = pos.source
        val start = pos.startOrPoint
        val length = pos.endOrPoint-start
        source.file match {
          case EclipseResource(i : IFile) =>
            FileUtils.task(i, tag, msg, priority, start, length, pos.line, null)
          case _ =>
        }
      }
    }
  }

  override def newCompiler(settings: Settings) = new EclipseBuildCompiler(settings, new BuildReporter(project))
  
  override def buildingFiles(included: scala.collection.Set[AbstractFile]) {
    for(file <- included) {
      file match {
        case EclipseResource(f : IFile) =>
          FileUtils.clearBuildErrors(f, null)
          FileUtils.clearTasks(f, null)
        case _ =>
      }
    }
  }
}
