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
package org.knime.knip.view3d;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Map;

import net.imagej.ImgPlus;

import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewFactory;
import org.knime.knip.view3d.image.Viewer3DNodeImageAdmin;
import org.knime.knip.view3d.image.Viewer3DNodeNotEnoughDimsException;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CellView3DFactory implements TableCellViewFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public TableCellView[] createTableCellViews() {
        if (!GraphicsEnvironment.isHeadless()) {
            if (Viewer3DNodeActivator.VTKLoaded()) {
                return new TableCellView[]{new TableCellView() {

                    Viewer3DNodeMain renderer = new Viewer3DNodeMain();

                    private final Map<ImgPlus, Viewer3DNodeImageAdmin> m_admins =
                            new HashMap<ImgPlus, Viewer3DNodeImageAdmin>();

                    @Override
                    public Component getViewComponent() {
                        // if there is an old renderer, delete it and create a
                        // new
                        // one
                        if (renderer != null) {
                            renderer.delete();
                        }

                        renderer = new Viewer3DNodeMain();

                        return renderer;
                    }

                    @Override
                    public void updateComponent(final DataValue valueToView) {
                        final ImgPlus imgPlus = ((ImgPlusValue)valueToView).getImgPlus();

                        Viewer3DNodeImageAdmin admin = null;

                        // see if the admins are still cached
                        if (m_admins.containsKey(imgPlus)) {
                            admin = m_admins.get(imgPlus);
                        } else {
                            try {
                                admin = new Viewer3DNodeImageAdmin(imgPlus, renderer.getEventService());
                            } catch (final Viewer3DNodeNotEnoughDimsException e) {
                                admin = null;
                            } finally {
                                m_admins.put(imgPlus, admin);
                            }
                        }

                        renderer.setAdmin(admin);
                    }

                    @Override
                    public void onClose() {
                        // delte all the admins
                        for (final Viewer3DNodeImageAdmin admin : m_admins.values()) {
                            if (admin != null) {
                                admin.delete();
                            }
                        }

                        // delete the renderer
                        renderer.delete();

                        renderer = null;
                        m_admins.clear();
                    }

                    @Override
                    public String getName() {
                        return "VTK 3D View";
                    }

                    @Override
                    public String getDescription() {
                        return "Allows one to render image in 3D. Please note if you are on a Linux machine with a NVIDIA GPU and more than one monitor please don't move the Viewer on a different monitor than the one it popped up at.";
                    };

                    @Override
                    public void loadConfigurationFrom(final ConfigRO config) {

                    }

                    @Override
                    public void saveConfigurationTo(final ConfigWO config) {

                    }

                    @Override
                    public void onReset() {

                    }

                }};

            } else {
                NodeLogger.getLogger(CellView3DFactory.class).warn("VTK not available.");
            }
        }
        return new TableCellView[0];

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends DataValue> getDataValueClass() {
        return ImgPlusValue.class;
    }

}
