package io.github.tassiLuca.boundary.impl

import gears.async.{Async, Task}
import io.github.tassiLuca.boundary.{BoundarySource, UpdatableBoundary}
import io.github.tassiLuca.core.Event.{ChangeDirection, Freeze}
import io.github.tassiLuca.core.{Event, RectangularEntities, Space2D}

import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{BorderLayout, Color, Graphics}
import javax.swing.border.LineBorder
import javax.swing.{JFrame, JPanel, SwingUtilities, WindowConstants}

class SwingUI(width: Int, height: Int) extends UpdatableBoundary[Space2D & RectangularEntities]:
  private val boundarySource = BoundarySource
  private val frame = JFrame("Popping Bubbles game")

  frame.addKeyListener(
    new KeyAdapter:
      override def keyPressed(e: KeyEvent): Unit = e.getKeyCode match
        case 38 | 40 => boundarySource.notifyListeners(ChangeDirection)
        case 32 => boundarySource.notifyListeners(Freeze)
        case _ => (),
  )

  override inline final def src: Async.Source[Event] = boundarySource

  override def asRunnable: Task[Unit] = Task {
    frame.setSize(width, height)
    frame.setVisible(true)
    frame.setLocationRelativeTo(null)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  }

  override def update(space: Space2D & RectangularEntities): Unit = SwingUtilities.invokeAndWait { () =>
    if frame.getContentPane.getComponentCount != 0 then frame.getContentPane.remove(0)
    frame.getContentPane.add(WorldPane(space, space.world.shape.width, space.world.shape.height), BorderLayout.CENTER)
    frame.getContentPane.repaint()
  }

  private class WorldPane(space: Space2D & RectangularEntities, width: Int, height: Int) extends JPanel:
    setSize(width, height)
    setBorder(LineBorder(Color.DARK_GRAY, 2))

    override def paintComponent(g: Graphics): Unit =
      val (px, py) = space.world.player.position
      g.fillRect(px, py, space.world.player.shape.width, space.world.player.shape.height)
      space.world.obstacles.foreach(o =>
        val (px, py) = o.position
        g.drawRect(px, py, o.shape.width, o.shape.height),
      )
