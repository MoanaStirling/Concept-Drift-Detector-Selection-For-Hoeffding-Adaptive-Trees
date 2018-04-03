package moa.streams.generators;

import java.util.Random;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

public class GradualDriftHyperplaneGenerator extends AbstractOptionHandler implements
        InstanceStream {

    @Override
    public String getPurposeString() {
        return "Generates a problem of predicting class of a rotating hyperplane.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public IntOption numClassesOption = new IntOption("numClasses", 'c',
            "The number of classes to generate.", 2, 2, Integer.MAX_VALUE);

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

    public IntOption numDriftAttsOption = new IntOption("numDriftAtts", 'k',
            "The number of attributes with drift.", 2, 0, Integer.MAX_VALUE);

    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'n', "Percentage of noise to add to the data.", 5, 0, 100);

    public IntOption sigmaPercentageOption = new IntOption("sigmaPercentage",
            's', "Percentage of probability that the direction of change is reversed.", 10, 0, 100);

    public IntOption speedOfChangeOption = new IntOption("instancesBeforeChange",
            'x', "Number of instances before concept drift occurs", 1000, 0, Integer.MAX_VALUE);

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected double[] weights;

    protected int[] sigma;

    protected double magnitude;

    protected double changeDirection = 1.0;

    protected int instances;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing hyperplane...", -1.0);
        generateHeader();
        restart();
    }

    protected void generateHeader() {
        FastVector attributes = new FastVector();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }

        FastVector classLabels = new FastVector();
        for (int i = 0; i < this.numClassesOption.getValue(); i++) {
            classLabels.addElement("class" + (i + 1));
        }
        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
    }

    @Override
    public long estimatedRemainingInstances() {
        return -1;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public boolean hasMoreInstances() {
        return true;
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public InstanceExample nextInstance() {

        int numAtts = this.numAttsOption.getValue();
        double[] attVals = new double[numAtts + 1];
        double sum = 0.0;
        double sumWeights = 0.0;
        for (int i = 0; i < numAtts; i++) {
            attVals[i] = this.instanceRandom.nextDouble();
            sum += this.weights[i] * attVals[i];
            sumWeights += this.weights[i];
        }
        int classLabel;
        if (sum >= sumWeights * 0.5) {
            classLabel = 1;
        } else {
            classLabel = 0;
        }
        //Add Noise
        if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
            classLabel = (classLabel == 0 ? 1 : 0);
        }

        Instance inst = new DenseInstance(1.0, attVals);
        inst.setDataset(getHeader());
        inst.setClassValue(classLabel);
        addDrift();
        incrementMagnitude();
        return new InstanceExample(inst);
    }

    private void addDrift() {
        for (int i = 0; i < this.numDriftAttsOption.getValue(); i++) {
            this.weights[i] += (double) ((double) sigma[i]) * ((double) this.magnitude);
            if (//this.weights[i] >= 1.0 || this.weights[i] <= 0.0 ||
                    (1 + (this.instanceRandom.nextInt(100))) <= this.sigmaPercentageOption.getValue()) {
                this.sigma[i] *= -1;
            }
        }
    }

    private void incrementMagnitude() {
        this.instances++;
        if( this.instances > speedOfChangeOption.getValue()){
            instances = 0;
            changeDirection *= -1.0;
        }
        this.weights[0] += changeDirection / ((double) speedOfChangeOption.getValue());
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        this.weights = new double[this.numAttsOption.getValue()];
        this.sigma = new int[this.numAttsOption.getValue()];
        this.weights[0] = 0;
        for (int i = 1; i < this.numAttsOption.getValue(); i++) {
            this.weights[i] = this.instanceRandom.nextDouble();
            this.sigma[i] = (i < this.numDriftAttsOption.getValue() ? 1 : 0);
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
