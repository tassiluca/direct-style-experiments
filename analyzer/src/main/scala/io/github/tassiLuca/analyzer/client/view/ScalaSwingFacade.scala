package io.github.tassiLuca.analyzer.client.view

import io.github.tassiLuca.analyzer.client.{AppController, OrganizationReport}

import java.awt.event.ActionListener
import java.awt.{BorderLayout, FlowLayout, LayoutManager}
import javax.swing.table.DefaultTableModel
import javax.swing.*

object ScalaSwingFacade:
  
  type Constraint = Any

  given Conversion[JComponent, (JComponent, Constraint)] = c => (c, ())

  def createPanel(comps: (JComponent, Constraint)*)(using layout: LayoutManager = FlowLayout()): JPanel =
    comps.foldLeft(JPanel(layout))((p, c) => { p.add(c._1, c._2); p })
