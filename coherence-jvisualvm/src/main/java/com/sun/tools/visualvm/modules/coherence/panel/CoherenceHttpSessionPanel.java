/*
 * File: CoherenceHttpSessionPanel.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.sun.tools.visualvm.modules.coherence.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.HttpSessionTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpSessionData;

/**
 * An implementation of an {@link AbstractCoherencePanel} to Coherence*Web
 * statistics.
 *
 * @author Tim Middleton
 */
public class CoherenceHttpSessionPanel extends AbstractCoherencePanel
{
    private static final long serialVersionUID = 9148388883048820567L;

    /**
     * The total amount of active space used.
     */
    private JTextField txtTotalApplications;

    /**
     * The current Max latency.
     */
    private JTextField txtMaxReapDuration;

    /**
     * The machine statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Map.Entry<Object, Data>> httpSessionData;

    /**
     * The {@link HttpSessionTableModel} to display HTTP session data.
     */
    protected HttpSessionTableModel tmodel;

    /**
     * The graph of session counts.
     */
    private SimpleXYChartSupport sessionCountGraph;

    /**
     * The graph of overflow session counts.
     */
    private SimpleXYChartSupport reapDurationGraph;


    /**
     * Create the layout for the {@link AbstractCoherencePanel}.
     * 
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceHttpSessionPanel(VisualVMModel model)
    {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Create the header panel
        JPanel pnlHeader = new JPanel();

        pnlHeader.setLayout(new FlowLayout());

        txtTotalApplications = getTextField(10, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_applications", txtTotalApplications));
        pnlHeader.add(txtTotalApplications);

        txtMaxReapDuration = getTextField(6, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_max_reap_duration", txtMaxReapDuration));
        pnlHeader.add(txtMaxReapDuration);

        // create the table
        tmodel = new HttpSessionTableModel(VisualVMModel.DataType.HTTP_SESSION.getMetadata());

        ExportableJTable table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, 200));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, HttpSessionData.SESSION_TIMEOUT, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, HttpSessionData.AVG_SESSION_SIZE, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, HttpSessionData.AVG_REAPED_SESSIONS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, HttpSessionData.AVG_REAP_DURATION, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table,
                                       HttpSessionData.LAST_REAP_DURATION_MAX,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, HttpSessionData.SESSION_UPDATES, new RenderHelper.IntegerRenderer());

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel      pnlTop     = new JPanel();

        pnlTop.setLayout(new BorderLayout());

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);
        pnlTop.add(scrollPane, BorderLayout.CENTER);

        pneSplit.add(pnlTop);

        // create a chart for the machine load averages
        sessionCountGraph = GraphHelper.createSessionCountGraph();
        reapDurationGraph = GraphHelper.createReapDurationGraph();

        JSplitPane pneSplitPlotter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        pneSplitPlotter.setResizeWeight(0.5);

        pneSplitPlotter.add(sessionCountGraph.getChart());
        pneSplitPlotter.add(reapDurationGraph.getChart());

        pneSplit.add(pneSplitPlotter);
        add(pneSplit);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
    {
        int          cTotalSessionCount  = 0;
        int          cTotalOverflowCount = 0;
        int          c                   = 0;
        long         cMaxMaxLatency      = 0L;
        long         cCurrentAverage     = 0L;

        final String FORMAT              = "%5d";

        if (httpSessionData != null)
        {
            for (Map.Entry<Object, Data> entry : httpSessionData)
            {
                c++;
                cTotalSessionCount  += (Integer) entry.getValue().getColumn(HttpSessionData.SESSION_COUNT);
                cTotalOverflowCount += (Integer) entry.getValue().getColumn(HttpSessionData.OVERFLOW_COUNT);

                long nDuration = (Long) entry.getValue().getColumn(HttpSessionData.LAST_REAP_DURATION_MAX);

                cCurrentAverage += nDuration;

                if (nDuration > cMaxMaxLatency)
                {
                    cMaxMaxLatency = nDuration;
                }
            }

            cCurrentAverage = (c == 0 ? 0 : cCurrentAverage / c);

            txtTotalApplications.setText(String.format(FORMAT, c));

        }
        else
        {
            txtTotalApplications.setText(String.format(FORMAT, 0));
        }

        GraphHelper.addValuesToSessionCountGraph(sessionCountGraph, cTotalSessionCount, cTotalOverflowCount);
        GraphHelper.addValuesToReapDurationGraph(reapDurationGraph, cMaxMaxLatency, cCurrentAverage);

        // GraphHelper.addValuesToPersistenceLatencyGraph(persistenceLatencyGraph, cLatencyAverage * 1000.0f);

        txtMaxReapDuration.setText(Long.toString(cMaxMaxLatency));

        tmodel.fireTableDataChanged();

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
    {
        httpSessionData = model.getData(VisualVMModel.DataType.HTTP_SESSION);

        if (httpSessionData != null)
        {
            tmodel.setDataList(httpSessionData);
        }

    }
}
