/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.view3d.render;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Timer;

import org.lwjgl.LWJGLException;

import vtk.vtkGenericJavaRenderWindow;
import vtk.vtkGenericRenderWindowInteractor;
import vtk.vtkInteractorObserver;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 * This class extends the LWJGLVTKCanvas to add the functionality that can be found in the vtkCanvas class. That is, it
 * adds a vtkGenericRenderWindowInteractor, a vtkPlaneWidget and a vtkBoxWidget.
 * 
 * @see LWJGLVTKCanvas
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
class LWJGLVTKInteractiveCanvas extends LWJGLVTKCanvas implements MouseListener, MouseMotionListener, KeyListener,
        MouseWheelListener {

    protected vtkGenericRenderWindowInteractor iren = new vtkGenericRenderWindowInteractor();

    protected Timer timer = new Timer(10, new DelayAction());

    protected int ctrlPressed = 0;

    protected int shiftPressed = 0;

    protected int lastX = 0;

    protected int lastY = 0;

    /**
     * This constructor reconstructs a LWJGLVTKInteractiveCanvas.
     * 
     * {@inheritDoc}
     * 
     * @see LWJGLVTKCanvas#LWJGLVTKInteractiveCanvas(vtkGenericJavaRenderWindow,vtkRenderer)
     */
    LWJGLVTKInteractiveCanvas(final vtkGenericJavaRenderWindow window, final vtkRenderer ren) throws LWJGLException {
        super(window, ren);
        ConstructorStuff();
    }

    /**
     * This constructor sets up a completly new LWJGLVTKInteractiveCanvas.
     * 
     * {@inheritDoc}
     * 
     * @see LWJGLVTKCanvas#LWJGLVTKInteractiveCanvas()
     */
    LWJGLVTKInteractiveCanvas() throws LWJGLException {
        super();
        ConstructorStuff();
    }

    /**
     * This method does all the stuff both constructors should do.
     */
    private void ConstructorStuff() {
        // Setup same interactor style than vtkPanel
        final vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
        iren.SetInteractorStyle(style);

        iren.SetRenderWindow(GetRenderWindow());
        iren.TimerEventResetsTimerOff();
        iren.AddObserver("CreateTimerEvent", this, "StartTimer");
        iren.AddObserver("DestroyTimerEvent", this, "DestroyTimer");
        iren.AddObserver("RenderEvent", this, "Render");
        iren.SetSize(200, 200);
        iren.ConfigureEvent();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent event) {
                // The Canvas is being resized, get the new size
                final int width = getWidth();
                final int height = getHeight();
                setSize(width, height);
            }
        });

        ren.SetBackground(1.0, 1.0, 1.0);

        // add the listeners
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        this.addKeyListener(this);
    }

    public void Delete() {
        iren = null;
    }

    public void Render() {
        this.render();
    }

    public void StartTimer() {
        if (timer.isRunning()) {
            timer.stop();
        }

        timer.setRepeats(true);
        timer.start();
    }

    public void DestroyTimer() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    public vtkGenericRenderWindowInteractor getRenderWindowInteractor() {
        return this.iren;
    }

    public void setInteractorStyle(final vtkInteractorObserver style) {
        iren.SetInteractorStyle(style);
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (ren.VisibleActorCount() == 0) {
            return;
        }
        lock();
        rw.SetDesiredUpdateRate(5.0);
        lastX = e.getX();
        lastY = e.getY();

        ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
        shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

        iren.SetEventInformationFlipY(e.getX(), e.getY(), ctrlPressed, shiftPressed, '0', 0, "0");

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            iren.LeftButtonPressEvent();
        } else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
            iren.MiddleButtonPressEvent();
        } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
            iren.RightButtonPressEvent();
        }
        unlock();
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        rw.SetDesiredUpdateRate(0.01);

        ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
        shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

        iren.SetEventInformationFlipY(e.getX(), e.getY(), ctrlPressed, shiftPressed, '0', 0, "0");

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            lock();
            iren.LeftButtonReleaseEvent();
            unlock();
        }

        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
            lock();
            iren.MiddleButtonReleaseEvent();
            unlock();
        }

        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
            lock();
            iren.RightButtonReleaseEvent();
            unlock();
        }
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        this.requestFocus();
        iren.SetEventInformationFlipY(e.getX(), e.getY(), 0, 0, '0', 0, "0");
        iren.EnterEvent();
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        iren.SetEventInformationFlipY(e.getX(), e.getY(), 0, 0, '0', 0, "0");
        iren.LeaveEvent();
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        lastX = e.getX();
        lastY = e.getY();

        ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
        shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

        iren.SetEventInformationFlipY(e.getX(), e.getY(), ctrlPressed, shiftPressed, '0', 0, "0");

        lock();
        iren.MouseMoveEvent();
        unlock();
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        if (ren.VisibleActorCount() == 0) {
            return;
        }

        ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
        shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

        iren.SetEventInformationFlipY(e.getX(), e.getY(), ctrlPressed, shiftPressed, '0', 0, "0");

        lock();
        iren.MouseMoveEvent();
        unlock();
    }

    @Override
    public void keyTyped(final KeyEvent e) {
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if (ren.VisibleActorCount() == 0) {
            return;
        }
        final char keyChar = e.getKeyChar();

        ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
        shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

        iren.SetEventInformationFlipY(lastX, lastY, ctrlPressed, shiftPressed, keyChar, 0, String.valueOf(keyChar));

        lock();
        iren.KeyPressEvent();
        iren.CharEvent();
        unlock();
    }

    @Override
    public void keyReleased(final KeyEvent e) {
    }

    private class DelayAction implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            lock();
            iren.TimerEvent();
            unlock();
        }
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        final int notches = e.getWheelRotation();

        lock();

        // mouse up
        if (notches < 0) {
            iren.MouseWheelForwardEvent();
        } else { // mouse down
            iren.MouseWheelBackwardEvent();
        }

        unlock();
    }

    @Override
    public void setSize(final int x, final int y) {
        super.setSize(x, y);
        lock();
        iren.SetSize(x, y);
        iren.ConfigureEvent();
        unlock();
    }
}
