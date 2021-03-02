package com.expd.arch.messaging.router.chart;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.MeterLegend;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.Range;
import org.jfree.ui.RefineryUtilities;

public class MeterChartDemo {

    /**
     * Starting point for the meter plot demonstration application.
     *
     * @param args used to specify the type and value.
     */
    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Usage: java TestMeter <type> <value>");
            System.err.println("Type:  0 = PIE");
            System.err.println("Type:  1 = CIRCLE");
            System.err.println("Type:  2 = CHORD");
        }
        MeterChartDemo h = new MeterChartDemo();
        double val = 85;
        DialShape dialShape = DialShape.CIRCLE;
        if (args.length > 0) {
            int type = Integer.parseInt(args[0]);
            if (type == 0) {
                dialShape = DialShape.PIE;
            } else if (type == 1) {
                dialShape = DialShape.CIRCLE;
            } else if (type == 0) {
                dialShape = DialShape.CHORD;
            }
        }
        if (args.length > 1) {
            val = new Double(args[1]).doubleValue();
        }
        h.displayMeterChart(val, dialShape);

    }

    /**
     * Displays a meter chart.
     *
     * @param value the value.
     * @param shape the dial shape.
     */
    void displayMeterChart(double value, DialShape shape) {

        DefaultValueDataset data = new DefaultValueDataset(2000.0);
        MeterPlot plot = new MeterPlot(data);
        plot.setUnits("records/minute");
        plot.setRange(new Range(0.0, 10000.0));
        plot.setNormalRange(new Range(0.0, 1000.0));
        plot.setWarningRange(new Range(1000.0, 5000.0));
        plot.setCriticalRange(new Range(5000.0, 10000.0));

        plot.setDialShape(shape);
        plot.setNeedlePaint(Color.white);
        plot.setTickLabelFont(new Font("SansSerif", Font.BOLD, 9));

        plot.setInsets(new Insets(5, 5, 5, 5));
        JFreeChart chart = new JFreeChart(
                "PropertyBasedRouter Throughput",
                JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                false
        );

        MeterLegend legend = new MeterLegend("Sample Meter");
        chart.setLegend(legend);

        chart.setBackgroundPaint(new GradientPaint(0, 0, Color.white, 0, 1000, Color.blue));

        JFrame chartFrame = new ChartFrame("PropertyBasedRouter Throughput", chart);
        chartFrame.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        chartFrame.pack();
        RefineryUtilities.positionFrameRandomly(chartFrame);
        chartFrame.setSize(400, 400);
        chartFrame.setVisible(true);

    }

}
