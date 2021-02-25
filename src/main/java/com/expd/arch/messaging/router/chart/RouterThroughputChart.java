package com.expd.arch.messaging.router.chart;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.MeterLegend;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.Range;
import org.jfree.ui.RefineryUtilities;
import com.expd.app.cdb.util.EDIRecordRateTracker;
import com.expd.arch.messaging.router.PropertyBasedRouter;

public class RouterThroughputChart implements ChartListener {
    private static long MAX_IDLE_BEFORE_RESET = 5000;
    private MeterPlot plot;
    private JFrame chartFrame;
    private long lastUpdate;
    PropertyBasedRouter router;

    public RouterThroughputChart(PropertyBasedRouter router) {
        this.router = router;
        EDIRecordRateTracker.current().setChartListener(this);
        this.displayMeterChart();
        this.startResetTimer();
    }

    /**
     * timer to reset throughput chart value to zero if data has
     * stopped coming in.
     */
    private void startResetTimer() {
        int delay = 5000; // initial delay of 5 seconds
        int period = 2000; // repeat every 2 seconds
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                long now = System.currentTimeMillis();
                if ((now - lastUpdate) > MAX_IDLE_BEFORE_RESET) {
                    setChartValue(0.0);
                }
            }
        }, delay, period);
    }

    public void setChartValue(double newValue) {
        this.plot.setDataset(new DefaultValueDataset(newValue));
        this.lastUpdate = System.currentTimeMillis();
    }

    /**
     * Displays a meter chart.
     * 
     * @param value
     *            the value.
     * @param shape
     *            the dial shape.
     */
    private void displayMeterChart() {
        DefaultValueDataset data = new DefaultValueDataset(0.0);
        this.plot = new MeterPlot(data);
        this.plot.setUnits("records/minute");
        this.plot.setRange(new Range(0.0, 100000.0));
        this.plot.setNormalRange(new Range(0.0, 20000.0));
        this.plot.setWarningRange(new Range(20000.0, 50000.0));
        this.plot.setCriticalRange(new Range(50000.0, 100000.0));
        this.plot.setDialShape(DialShape.CIRCLE);
        plot.setNeedlePaint(Color.white);
        plot.setTickLabelFont(new Font("SansSerif", Font.BOLD, 9));
        plot.setInsets(new Insets(5, 5, 5, 5));
        String chartTitle = "EDI records per minute inbound on: "
                + this.router.getInboundQueue();
        JFreeChart chart = new JFreeChart(chartTitle,
                JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        MeterLegend legend = new MeterLegend("Sample Meter");
        chart.setLegend(legend);
        chart.setBackgroundPaint(new GradientPaint(0, 0, Color.white, 0, 1000,
                Color.blue));
        this.chartFrame = new ChartFrame("PropertyBasedRouter Throughput",
                chart);
        this.chartFrame.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window is in the process of being
             * closed. The close operation can be overridden at this
             * point.
             */
            public void windowClosing(WindowEvent e) {
                router.removeThroughputChart();
                System.out.println("CLOSED Router Throughput Chart Window");
                ;
            }
        });
        chartFrame.pack();
        RefineryUtilities.positionFrameRandomly(chartFrame);
        chartFrame.setSize(400, 400);
        chartFrame.setVisible(true);
    }

    public void close() {
        if (this.chartFrame != null) {
            this.chartFrame.dispose();
        }
        EDIRecordRateTracker.current().setChartListener(null);
    }
}