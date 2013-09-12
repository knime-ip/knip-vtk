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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.NodeLogger;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.view3d.image.Viewer3DNodeVolume;
import org.lwjgl.LWJGLException;

import vtk.vtk3DWidget;
import vtk.vtkGenericRenderWindowInteractor;
import vtk.vtkInteractorObserver;
import vtk.vtkInteractorStyle;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkProp;
import vtk.vtkRenderWindow;
import vtk.vtkRenderer;

/**
 * This class is an abstract base for all Renderer.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public abstract class Viewer3DNodeRenderer extends ViewerComponent {

    /**
     * Eclipse generated.
     */

    private static final long serialVersionUID = -928144289645440726L;

    // These two are the java wrappers
    private LWJGLVTKInteractiveCanvas m_renderWindowCanvas = null;

    // while this is the real vtkRenderWindow
    private vtkRenderWindow m_renderWindow = null;

    private vtkRenderer m_renderer = null;

    // the EventService to use
    protected EventService m_eventService = null;

    // The camera state to use for resetting
    private double m_cameraPosition[] = null;

    private double m_viewUp[] = null;

    // The LOGGER
    protected static final NodeLogger LOGGER = NodeLogger.getLogger(Viewer3DNodeRenderer.class);

    // Lists of widgets etc
    private List<vtk3DWidget> m_widgets = new LinkedList<vtk3DWidget>();

    private List<RenderWindowDependent> m_renderWindowDependents = new LinkedList<RenderWindowDependent>();

    private List<Viewer3DNodeVolume> m_volumes = new LinkedList<Viewer3DNodeVolume>();

    private HierarchyListener m_hierarchyListener;

    /**
     * Set up a new MainRenderer.
     * 
     * @param eventService The event service to use
     * @param message The message to display on the border
     * @param border hide the border or not
     */
    public Viewer3DNodeRenderer(final EventService eventService, final String message, final boolean border) {
        super(message, border);
        setEventService(eventService);

        setLayout(new BorderLayout());
        add(setUpRenderWindow(), BorderLayout.CENTER);

        // save the inital camera position
        m_cameraPosition = m_renderer.GetActiveCamera().GetPosition();
        m_viewUp = m_renderer.GetActiveCamera().GetViewUp();

        // on linux with openjdk
        // add a listener that informs us if the panel is going to be shown
        // again, and use this to exchange the LWJGLVTKCanvas
        if (System.getProperty("os.name").toLowerCase().contains("linux")
                && System.getProperty("java.runtime.name").toLowerCase().contains("openjdk")) {
            m_hierarchyListener = new HierarchyListener() {

                @Override
                public void hierarchyChanged(final HierarchyEvent e) {
                    // only act on visibilty changes
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {

                        // only change if we become visible
                        if (isShowing()) {
                            remove(m_renderWindowCanvas);
                            add(changeRenderWindow(), BorderLayout.CENTER);
                        }
                    }
                }
            };
            this.addHierarchyListener(m_hierarchyListener);
        }

    }

    /**
     * This method exchanges the current LWJGLVTKCanvas.<br>
     * 
     * It also reconnects all registered widgets to the new Renderer.
     * 
     * @return the new renderwindow
     */
    private Component changeRenderWindow() {
        // deactivate all widgets to prevent a crash on switching back and forth
        // between tabs with activated and modified widget
        for (final vtk3DWidget w : m_widgets) {
            w.Off();
        }

        final vtkInteractorObserver style = m_renderWindowCanvas.getRenderWindowInteractor().GetInteractorStyle();
        m_cameraPosition = m_renderer.GetActiveCamera().GetPosition();
        m_viewUp = m_renderer.GetActiveCamera().GetViewUp();

        m_renderer.RemoveAllViewProps();

        // delete the old stuff
        m_renderWindow.GetInteractor().Delete();
        m_renderer.Delete();
        m_renderWindow.Delete();

        // create the new Stuff
        setUpRenderWindow();
        m_renderWindowCanvas.setInteractorStyle(style);

        // readd the volumes
        if (m_volumes.size() > 0) {
            addViewProps(m_volumes);
        }

        // reset the camera
        m_renderer.GetActiveCamera().SetPosition(m_cameraPosition);
        m_renderer.GetActiveCamera().SetViewUp(m_viewUp);

        // reset the widgets
        for (final vtk3DWidget w : m_widgets) {
            w.SetInteractor(m_renderWindowCanvas.getRenderWindowInteractor());
        }

        // inform all dependents
        for (final RenderWindowDependent d : m_renderWindowDependents) {
            d.renderWindowChanged(m_renderWindow);
        }

        return m_renderWindowCanvas;
    }

    /**
     * This method sets up the renderWindow for the first time.<br>
     * 
     * @return the renderwindow
     */
    private Component setUpRenderWindow() {
        final vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();

        try {
            m_renderWindowCanvas = new LWJGLVTKInteractiveCanvas();
            m_renderWindowCanvas.setInteractorStyle(style);
            m_renderer = m_renderWindowCanvas.GetRenderer();
            m_renderWindow = m_renderWindowCanvas.GetRenderWindow();
        } catch (final LWJGLException e) {
            // TODO Terminate here
            LOGGER.error("Could not initalize render window! " + e.getMessage());
            LOGGER.error(e.getStackTrace());
        }

        m_renderer.SetBackground(1.0, 1.0, 1.0);

        return m_renderWindowCanvas;
    }

    /**
     * Set the volumes to be rendered.<br>
     * 
     * The actual behavious has to be defined in the subclasses
     * 
     * @param volumes the volumes to show
     */
    public void setVolumes(final List<Viewer3DNodeVolume> volumes) {
        // TODO NULL check
        m_volumes = volumes;
        final List<vtkProp> props = addViewProps(m_volumes);

        m_renderer.RemoveAllViewProps();
        for (final vtkProp p : props) {
            m_renderer.AddViewProp(p);
        }

        // Reset the camera to fit to props
        m_renderer.ResetCamera();

        // Save the new positions
        m_cameraPosition = m_renderer.GetActiveCamera().GetPosition();
        m_viewUp = m_renderer.GetActiveCamera().GetViewUp();
    }

    /**
     * Extract the ViewProps to display from the volume and return them to the caller.<br>
     * 
     * @param volumes a list of all volumes to display
     * 
     * @return a list of all vtkProps to display
     */
    protected abstract List<vtkProp> addViewProps(final List<Viewer3DNodeVolume> volumes);

    /**
     * Use this method to register a widget for this window.<br>
     * 
     * It will be connected to the current Interactor and it is also made sure that it will be connected to the
     * Interactor after the LWJGLVTKCanvas has been exchanged.
     * 
     * @param widget the widget
     */
    protected final void registerWidget(final vtk3DWidget widget) {
        widget.SetInteractor(m_renderWindowCanvas.getRenderWindowInteractor());
        m_widgets.add(widget);
    }

    /**
     * Remove a widget from this window.
     * 
     * @param widget the widget
     */
    protected final void removeWidget(final vtk3DWidget widget) {
        m_widgets.remove(widget);
    }

    /**
     * Render the image.
     */
    public final void render() {
        paint(getGraphics());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.swing.JComponent#paint(Graphics)
     */
    @Override
    public final void paint(final Graphics g) {
        super.paint(g);
        m_renderWindowCanvas.render();
    }

    /**
     * Set the interactor style to use for this Renderer.
     * 
     * @param style the style
     */
    protected final void setInteractorStyle(final vtkInteractorStyle style) {
        m_renderWindowCanvas.setInteractorStyle(style);
    }

    /**
     * Add the passed prop to the props that are being rendered.
     * 
     * @param prop the prop to render
     */
    protected final void addProp(final vtkProp prop) {
        m_renderer.AddViewProp(prop);
    }

    /**
     * Remove the passed prop from the props that are being rendered.
     * 
     * @param prop the prop to remove
     */
    protected final void removeProp(final vtkProp prop) {
        m_renderer.RemoveViewProp(prop);
    }

    /**
     * Delete the vtkObjects so that they can be collected by using the vtk Garbage Collector.
     */
    public final void delete() {
        deleteAdditional();

        // remove the viewprops so we can safely delete them
        m_renderer.RemoveAllViewProps();

        // delete the window
        m_renderWindowCanvas.Delete();

        this.removeHierarchyListener(m_hierarchyListener);

        this.m_eventService = null;
        this.m_renderer.RemoveAllObservers();
        this.m_renderer.RemoveAllViewProps();
        this.m_volumes.clear();
        this.m_volumes = null;
        this.m_renderer = null;
        this.m_renderWindowCanvas = null;
        this.m_widgets = null;
        this.m_renderWindowDependents.clear();
        this.m_renderWindowDependents = null;
    }

    /**
     * Use this method to delete any additional added vtkObjects.<br>
     * 
     * Will always be called first in the delete() method.
     */
    protected abstract void deleteAdditional();

    /**
     * Get the current Interactor.
     * 
     * @return interactor
     */
    protected final vtkGenericRenderWindowInteractor getRenderWindowInteractor() {
        return m_renderWindowCanvas.getRenderWindowInteractor();
    }

    /**
     * Use this method to register a new RenderWindowDependent.<br>
     * 
     * The method renderWindowChanged will then be called once immediatly.
     * 
     * @param dep the dependent
     */
    public final void addRenderWindowDependent(final RenderWindowDependent dep) {
        if (dep != null) {
            m_renderWindowDependents.add(dep);
            dep.renderWindowChanged(m_renderWindow);
        }
    }

    /**
     * Remove a RenderWindowDependent.
     * 
     * @param dep the dependent
     */
    public final void removeRenderWindowDependent(final RenderWindowDependent dep) {
        if (dep != null) {
            m_renderWindowDependents.remove(dep);
        }
    }

    @Override
    public final void setEventService(final EventService eventService) {
        if (eventService != null) {
            m_eventService = eventService;
        } else {
            m_eventService = new EventService();
        }
        m_eventService.subscribe(this);
    }

    @Override
    public final Position getPosition() {
        // not used
        return null;
    }

    @Override
    public final void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // not used
    }

    @Override
    public final void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // not used
    }

    /**
     * Reset the camera to standard value.
     */
    public final void resetCamera() {
        m_renderer.ResetCamera();
        m_renderer.GetActiveCamera().SetPosition(m_cameraPosition);
        m_renderer.GetActiveCamera().SetViewUp(m_viewUp);
        render();
    }
}
