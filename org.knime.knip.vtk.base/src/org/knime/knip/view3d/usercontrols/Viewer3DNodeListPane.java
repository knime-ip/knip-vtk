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
package org.knime.knip.view3d.usercontrols;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;

/**
 * This class wraps a JList inside a JScrollPane.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeListPane extends JPanel {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = -1171273115037025158L;

    private final JList m_list;

    private final JScrollPane m_pane;

    /**
     * Set up a new pane, using the given values.
     * 
     * @param values values to display
     */
    public Viewer3DNodeListPane(final String[] values) {
        // Set up the list
        m_list = new JList();
        m_list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        m_list.setVisibleRowCount(1);
        m_list.setFixedCellWidth(-1);
        setData(values);

        // Set up the ScrollPane
        m_pane = new JScrollPane(m_list);
        m_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        m_pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(m_pane);
    }

    /**
     * Set the data that should be displayed.
     * 
     * @param data the data to display
     */
    public final void setData(final String[] data) {
        if ((data != null) && (data.length > 0)) {
            m_list.setListData(data);
            m_list.setPrototypeCellValue(data[data.length - 1]);
            m_list.setSelectedIndex(0);
        }
    }

    /**
     * Get the currently selected Index of the JList.
     * 
     * @return the index
     */
    public final int getSelectedIndex() {
        return m_list.getSelectedIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see JComponent#setEnabled(boolean)
     */
    @Override
    public final void setEnabled(final boolean enable) {
        m_list.setEnabled(enable);
    }

    /**
     * Set the JList to allow only single selections.
     */
    public final void singleSelection() {
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Set the JList to allow multiple selections.
     */
    public final void generalSelection() {
        m_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    /**
     * Add a Listener to the List.
     * 
     * @param listener the listener
     */
    public final void addListSelectionListener(final ListSelectionListener listener) {
        m_list.addListSelectionListener(listener);
    }

    /**
     * @see JList#removeListSelectionListener(ListSelectionListener)
     */
    public void removeListSelectionListener(final ListSelectionListener listener) {
        m_list.removeListSelectionListener(listener);
    }

    /**
     * @see JList#getSelectedIndices()
     */
    public int[] getSelectedIndices() {
        return m_list.getSelectedIndices();
    }

    /**
     * @see JList#setSelectedIndex(int)
     */
    public void setSelectedIndex(final int index) {
        m_list.setSelectedIndex(index);
    }

    /**
     * @see JList#setSelectedIndices(int[])
     */
    public void setSelectedIndices(final int[] arg0) {
        m_list.setSelectedIndices(arg0);
    }

    /**
     * @see JList#getSelectedValue()
     */
    public Object getSelectedValue() {
        return m_list.getSelectedValue();
    }
}
