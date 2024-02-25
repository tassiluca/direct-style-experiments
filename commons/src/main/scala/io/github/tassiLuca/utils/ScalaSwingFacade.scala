package io.github.tassiLuca.utils

import java.awt.{Component, FlowLayout, LayoutManager}
import javax.swing.{JComponent, JFrame, JPanel, WindowConstants}

object ScalaSwingFacade:

  private type Constraint = Any

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

  extension (f: JFrame)
    def display(): Unit =
      f.pack()
      f.setVisible(true)
      f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      f.setLocationRelativeTo(null)
