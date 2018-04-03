package moa.streams.generators;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import moa.core.FastVector;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.Random;
import moa.core.Example;
import moa.core.InstanceExample;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

public class AgrawalDriftGenerator extends AbstractOptionHandler implements
        InstanceStream {

    @Override
    public String getPurposeString() {
        return "Generates one of ten different pre-defined loan functions.";
    }

    private static final long serialVersionUID = 1L;

    protected int instanceCount;

    protected int function;

    /*public IntOption functionOption = new IntOption("function", 'f',
            "Classification function used, as defined in the original paper.",
            1, 1, 10);*/

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public FloatOption peturbFractionOption = new FloatOption("peturbFraction",
            'p',
            "The amount of peturbation (noise) introduced to numeric values.",
            0.05, 0.0, 1.0);

    public FlagOption balanceClassesOption = new FlagOption("balanceClasses",
            'b', "Balance the number of instances of each class.");

    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'n', "Percentage of noise to add to the data.", 10, 0, 100);

    public IntOption instancesBeforeChangeOption = new IntOption("instancesBeforeChange",
            's', "Number of instances before classification function is changed", 100000, 0, Integer.MAX_VALUE);

    protected interface ClassFunction {

        public int determineClass(double salary, double commission, int age,
                int elevel, int car, int zipcode, double hvalue, int hyears,
                double loan);
    }

    protected static ClassFunction[] classificationFunctions = {
        // function 1
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        return ((age < 40) || (60 <= age)) ? 0 : 1;
    }
},
        // function 2
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            return ((50000 <= salary) && (salary <= 100000)) ? 0
                    : 1;
        } else if (age < 60) {// && age >= 40
            return ((75000 <= salary) && (salary <= 125000)) ? 0
                    : 1;
        } else {// age >= 60
            return ((25000 <= salary) && (salary <= 75000)) ? 0 : 1;
        }
    }
},
        // function 3
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            return ((elevel == 0) || (elevel == 1)) ? 0 : 1;
        } else if (age < 60) { // && age >= 40
            return ((elevel == 1) || (elevel == 2) || (elevel == 3)) ? 0
                    : 1;
        } else { // age >= 60
            return ((elevel == 2) || (elevel == 3) || (elevel == 4)) ? 0
                    : 1;
        }
    }
},
        // function 4
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            if ((elevel == 0) || (elevel == 1)) {
                return ((25000 <= salary) && (salary <= 75000)) ? 0
                        : 1;
            }
            return ((50000 <= salary) && (salary <= 100000)) ? 0
                    : 1;
        } else if (age < 60) {// && age >= 40
            if ((elevel == 1) || (elevel == 2) || (elevel == 3)) {
                return ((50000 <= salary) && (salary <= 100000)) ? 0
                        : 1;
            }
            return ((75000 <= salary) && (salary <= 125000)) ? 0
                    : 1;
        } else {// age >= 60
            if ((elevel == 2) || (elevel == 3) || (elevel == 4)) {
                return ((50000 <= salary) && (salary <= 100000)) ? 0
                        : 1;
            }
            return ((25000 <= salary) && (salary <= 75000)) ? 0 : 1;
        }
    }
},
        // function 5
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        if (age < 40) {
            if ((50000 <= salary) && (salary <= 100000)) {
                return ((100000 <= loan) && (loan <= 300000)) ? 0
                        : 1;
            }
            return ((200000 <= loan) && (loan <= 400000)) ? 0 : 1;
        } else if (age < 60) {// && age >= 40
            if ((75000 <= salary) && (salary <= 125000)) {
                return ((200000 <= loan) && (loan <= 400000)) ? 0
                        : 1;
            }
            return ((300000 <= loan) && (loan <= 500000)) ? 0 : 1;
        } else {// age >= 60
            if ((25000 <= salary) && (salary <= 75000)) {
                return ((300000 <= loan) && (loan <= 500000)) ? 0
                        : 1;
            }
            return ((100000 <= loan) && (loan <= 300000)) ? 0 : 1;
        }
    }
},
        // function 6
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double totalSalary = salary + commission;
        if (age < 40) {
            return ((50000 <= totalSalary) && (totalSalary <= 100000)) ? 0
                    : 1;
        } else if (age < 60) {// && age >= 40
            return ((75000 <= totalSalary) && (totalSalary <= 125000)) ? 0
                    : 1;
        } else {// age >= 60
            return ((25000 <= totalSalary) && (totalSalary <= 75000)) ? 0
                    : 1;
        }
    }
},
        // function 7
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double disposable = (2.0 * (salary + commission) / 3.0
                - loan / 5.0 - 20000.0);
        return disposable > 0 ? 0 : 1;
    }
},
        // function 8
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double disposable = (2.0 * (salary + commission) / 3.0
                - 5000.0 * elevel - 20000.0);
        return disposable > 0 ? 0 : 1;
    }
},
        // function 9
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double disposable = (2.0 * (salary + commission) / 3.0
                - 5000.0 * elevel - loan / 5.0 - 10000.0);
        return disposable > 0 ? 0 : 1;
    }
},
        // function 10
        new ClassFunction() {

    @Override
    public int determineClass(double salary, double commission,
            int age, int elevel, int car, int zipcode,
            double hvalue, int hyears, double loan) {
        double equity = 0.0;
        if (hyears >= 20) {
            equity = hvalue * (hyears - 20.0) / 10.0;
        }
        double disposable = (2.0 * (salary + commission) / 3.0
                - 5000.0 * elevel + equity / 5.0 - 10000.0);
        return disposable > 0 ? 0 : 1;
    }
}};

    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected boolean nextClassShouldBeZero;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // generate header
        FastVector attributes = new FastVector();
        attributes.addElement(new Attribute("salary"));
        attributes.addElement(new Attribute("commission"));
        attributes.addElement(new Attribute("age"));
        FastVector elevelLabels = new FastVector();
        for (int i = 0; i < 5; i++) {
            elevelLabels.addElement("level" + i);
        }
        attributes.addElement(new Attribute("elevel", elevelLabels));
        FastVector carLabels = new FastVector();
        for (int i = 0; i < 20; i++) {
            carLabels.addElement("car" + (i + 1));
        }
        attributes.addElement(new Attribute("car", carLabels));
        FastVector zipCodeLabels = new FastVector();
        for (int i = 0; i < 9; i++) {
            zipCodeLabels.addElement("zipcode" + (i + 1));
        }
        attributes.addElement(new Attribute("zipcode", zipCodeLabels));
        attributes.addElement(new Attribute("hvalue"));
        attributes.addElement(new Attribute("hyears"));
        attributes.addElement(new Attribute("loan"));
        FastVector classLabels = new FastVector();
        classLabels.addElement("groupA");
        classLabels.addElement("groupB");
        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
        restart();
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

        instanceCount++;
        if(instanceCount > instancesBeforeChangeOption.getValue()){
            instanceCount = 0;
            function++;
            if(function > 9){
                function = 0;
            }
        }

        double salary = 0, commission = 0, hvalue = 0, loan = 0;
        int age = 0, elevel = 0, car = 0, zipcode = 0, hyears = 0, group = 0;
        boolean desiredClassFound = false;
        while (!desiredClassFound) {
            // generate attributes
            salary = 20000.0 + 130000.0 * this.instanceRandom.nextDouble();
            commission = (salary >= 75000.0) ? 0
                    : (10000.0 + 65000.0 * this.instanceRandom.nextDouble());
            // true to c implementation:
            // if (instanceRandom.nextDouble() < 0.5 && salary < 75000.0)
            // commission = 10000.0 + 65000.0 * instanceRandom.nextDouble();
            age = 20 + this.instanceRandom.nextInt(61);
            elevel = this.instanceRandom.nextInt(5);
            car = this.instanceRandom.nextInt(20);
            zipcode = this.instanceRandom.nextInt(9);
            hvalue = (9.0 - zipcode) * 100000.0
                    * (0.5 + this.instanceRandom.nextDouble());
            hyears = 1 + this.instanceRandom.nextInt(30);
            loan = this.instanceRandom.nextDouble() * 500000.0;
            // determine class
            group = classificationFunctions[this.function].determineClass(salary, commission, age, elevel, car,
                    zipcode, hvalue, hyears, loan);
            if (!this.balanceClassesOption.isSet()) {
                desiredClassFound = true;
            } else {
                // balance the classes
                if ((this.nextClassShouldBeZero && (group == 0))
                        || (!this.nextClassShouldBeZero && (group == 1))) {
                    desiredClassFound = true;
                    this.nextClassShouldBeZero = !this.nextClassShouldBeZero;
                } // else keep searching
            }
        }
        // perturb values
        if (this.peturbFractionOption.getValue() > 0.0) {
            salary = perturbValue(salary, 20000, 150000);
            if (commission > 0) {
                commission = perturbValue(commission, 10000, 75000);
            }
            age = (int) Math.round(perturbValue(age, 20, 80));
            hvalue = perturbValue(hvalue, (9.0 - zipcode) * 100000.0, 0, 135000);
            hyears = (int) Math.round(perturbValue(hyears, 1, 30));
            loan = perturbValue(loan, 0, 500000);
        }

        if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
            group = (group == 0 ? 1 : 0);
        }

        // construct instance
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setValue(0, salary);
        inst.setValue(1, commission);
        inst.setValue(2, age);
        inst.setValue(3, elevel);
        inst.setValue(4, car);
        inst.setValue(5, zipcode);
        inst.setValue(6, hvalue);
        inst.setValue(7, hyears);
        inst.setValue(8, loan);
        inst.setDataset(header);
        inst.setClassValue(group);
        return new InstanceExample(inst);
    }

    protected double perturbValue(double val, double min, double max) {
        return perturbValue(val, max - min, min, max);
    }

    protected double perturbValue(double val, double range, double min,
            double max) {
        val += range * (2.0 * (this.instanceRandom.nextDouble() - 0.5))
                * this.peturbFractionOption.getValue();
        if (val < min) {
            val = min;
        } else if (val > max) {
            val = max;
        }
        return val;
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        this.nextClassShouldBeZero = false;
        this.instanceCount = 0;
        this.function = 0;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
