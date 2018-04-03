package moa.classifiers.core.driftdetection;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class PageHinkley extends AbstractChangeDetector {

    private static final long serialVersionUID = -3518369648142099719L;

    public IntOption minNumInstancesOption = new IntOption(
            "minNumInstances",
            'n',
            "The minimum number of instances before permitting detecting change.",
            30, 0, Integer.MAX_VALUE);

    public FloatOption deltaOption = new FloatOption("delta", 'd',
            "Delta parameter of the Page Hinkley Test", 0.005, 0.0, 1.0);

    public FloatOption lambdaOption = new FloatOption("lambda", 'l',
            "Lambda parameter of the Page Hinkley Test", 50, 0.0, Float.MAX_VALUE);

    public FloatOption alphaOption = new FloatOption("alpha", 'a',
            "Alpha parameter of the Page Hinkley Test", 1 - 0.0001, 0.0, 1.0);

    private int m_n;

    private double sum;

    private double x_mean;

    private double alpha;

    private double delta;

    private double lambda;

    private int minNumInstances;

    public PageHinkley(double delta, double alpha, double lambda, int minNumInstances) {
        resetLearning(delta, alpha, lambda, minNumInstances);
    }

    public PageHinkley() {
        resetLearning();
    }

    public void resetLearning(double delta, double alpha, double lambda, int minNumInstances) {
        m_n = 1;
        x_mean = 0.0;
        sum = 0.0;
        this.delta = delta;
        this.alpha = alpha;
        this.lambda = lambda;
        this.minNumInstances = minNumInstances;
    }

    @Override
    public void resetLearning() {
        m_n = 1;
        x_mean = 0.0;
        sum = 0.0;
        this.delta = this.deltaOption.getValue();
        this.alpha = this.alphaOption.getValue();
        this.lambda = this.lambdaOption.getValue();
        this.minNumInstances = this.minNumInstancesOption.getValue();
    }

    @Override
    public void input(double x) {
        // It monitors the error rate
        if (this.isChangeDetected == true || this.isInitialized == false) {
            resetLearning();
            this.isInitialized = true;
        }

        x_mean = x_mean + (x - x_mean) / (double) m_n;
        sum = this.alpha * sum + (x - x_mean - this.delta);

        m_n++;

        // System.out.print(prediction + " " + m_n + " " + (m_p+m_s) + " ");
        this.estimation = x_mean;
        this.isChangeDetected = false;
        this.isWarningZone = false;
        this.delay = 0;

        if (m_n < this.minNumInstances) {
            return;
        }

        if (sum > this.lambda) {
            this.isChangeDetected = true;
        } 
    }

    public boolean setInput(double value){
        this.input(value);
        return this.isChangeDetected;
    }

    public double getWidth(){ return this.m_n; }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}
