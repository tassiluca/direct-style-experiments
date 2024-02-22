package io.github.tassiLuca.utils

import java.awt.{Component, FlowLayout, LayoutManager}
import javax.swing.{JComponent, JPanel}

object ScalaSwingFacade:

  type Constraint = Any

  given Conversion[JComponent, (JComponent, Constraint)] = c => (c, ())

  def createPanel(comps: (JComponent, Constraint)*)(using
      layout: LayoutManager = FlowLayout(FlowLayout.CENTER),
  ): JPanel = comps.foldLeft(JPanel(layout))((p, c) => { p.add(c._1, c._2); p })

  extension (p: JPanel)
    def addWithRepaint(c: Component): JPanel =
      p.add(c)
      p.revalidate()
      p.repaint()
      p
