
package moa.classifiers.trees;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.DoubleVector;
import moa.core.MiscUtils;
import moa.core.Utils;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Measurement;

public class HoeffdingAdaptiveTree extends HoeffdingTree {

    int instanceCount = 0;

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Hoeffding Adaptive Tree for evolving data streams that uses ADWIN to replace branches for new ones.";
    }
    
 /*   public MultiChoiceOption leafpredictionOption = new MultiChoiceOption(
            "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                "MC", "NB", "NBAdaptive"}, new String[]{
                "Majority class",
                "Naive Bayes",
                "Naive Bayes Adaptive"}, 2);*/

    public interface NewNode {

        // Change for adwin
        //public boolean getErrorChange();
        public int numberLeaves();

        public double getErrorEstimation();

        public double getErrorWidth();

        public boolean isNullError();

        public void killTreeChilds(HoeffdingAdaptiveTree ht);

        public void learnFromInstance(Instance inst, HoeffdingAdaptiveTree ht, SplitNode parent, int parentBranch);

        public void filterInstanceToLeaves(Instance inst, SplitNode myparent, int parentBranch, List<FoundNode> foundNodes,
                boolean updateSplitterCounts);
    }

    public static class AdaSplitNode extends SplitNode implements NewNode {

        private static final long serialVersionUID = 1L;

        protected Node alternateTree;

        protected ADWIN estimationErrorWeight;
        //public boolean isAlternateTree = false;

        public boolean ErrorChange = false;

        protected int randomSeed = 1;

        protected Random classifierRandom;

        //public boolean getErrorChange() {
        //		return ErrorChange;
        //}
        @Override
        public int calcByteSizeIncludingSubtree() {
            int byteSize = calcByteSize();
            if (alternateTree != null) {
                byteSize += alternateTree.calcByteSizeIncludingSubtree();
            }
            if (estimationErrorWeight != null) {
                byteSize += estimationErrorWeight.measureByteSize();
            }
            for (Node child : this.children) {
                if (child != null) {
                    byteSize += child.calcByteSizeIncludingSubtree();
                }
            }
            return byteSize;
        }
        
        public AdaSplitNode(InstanceConditionalTest splitTest,
                double[] classObservations, int size) {
            super(splitTest, classObservations, size);
            this.classifierRandom = new Random(this.randomSeed);
        }
        
        public AdaSplitNode(InstanceConditionalTest splitTest,
                double[] classObservations) {
            super(splitTest, classObservations);
            this.classifierRandom = new Random(this.randomSeed);
        }

        @Override
        public int numberLeaves() {
            int numLeaves = 0;
            for (Node child : this.children) {
                if (child != null) {
                    numLeaves += ((NewNode) child).numberLeaves();
                }
            }
            return numLeaves;
        }

        @Override
        public double getErrorEstimation() {
            return this.estimationErrorWeight.getEstimation();
        }

        @Override
        public double getErrorWidth() {
            double w = 0.0;
            if (isNullError() == false) {
                w = this.estimationErrorWeight.getWidth();
            }
            return w;
        }

        @Override
        public boolean isNullError() {
            return (this.estimationErrorWeight == null);
        }

        // SplitNodes can have alternative trees, but LearningNodes can't
        // LearningNodes can split, but SplitNodes can't
        // Parent nodes are allways SplitNodes
        @Override
        public void learnFromInstance(Instance inst, HoeffdingAdaptiveTree ht, SplitNode parent, int parentBranch) {


            int trueClass = (int) inst.classValue();
            //New option vore
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            Instance weightedInst = (Instance) inst.copy();
            if (k > 0) {
                //weightedInst.setWeight(inst.weight() * k);
            }
            //Compute ClassPrediction using filterInstanceToLeaf
            //int ClassPrediction = Utils.maxIndex(filterInstanceToLeaf(inst, null, -1).node.getClassVotes(inst, ht));
            int ClassPrediction = 0;
            if (filterInstanceToLeaf(inst, parent, parentBranch).node != null) {
                ClassPrediction = Utils.maxIndex(filterInstanceToLeaf(inst, parent, parentBranch).node.getClassVotes(inst, ht));
            }

            boolean blCorrect = (trueClass == ClassPrediction);

            if (this.estimationErrorWeight == null) {
                this.estimationErrorWeight = new ADWIN();
            }
            double oldError = this.getErrorEstimation();
            double oldWidth = this.getErrorWidth();
            this.ErrorChange = this.estimationErrorWeight.setInput(blCorrect == true ? 0.0 : 1.0);
            if (this.ErrorChange == true && oldError > this.getErrorEstimation()) {
                //if error is decreasing, don't do anything
                this.ErrorChange = false;
            }

            // Check condition to build a new alternate tree
            //if (this.isAlternateTree == false) {
            if (this.ErrorChange == true) {//&& this.alternateTree == null) {
                //Start a new alternative tree : learning node
                this.alternateTree = ht.newLearningNode();
                //this.alternateTree.isAlternateTree = true;
                ht.alternateTrees++;
                System.out.print("Instance: ");
                System.out.println(ht.instanceCount);
                System.out.print("Subtree Depth: ");
                System.out.println(subtreeDepth());
                System.out.print("Old Error Estimation: ");
                System.out.println(oldError);
                System.out.print("New Error Estimation: ");
                System.out.println(this.getErrorEstimation());
                System.out.print("Old Error Width: ");
                System.out.println(oldWidth);
                System.out.print("New Error Width: ");
                System.out.println(this.getErrorWidth());
                System.out.println();
            } // Check condition to replace tree
            else if (this.alternateTree != null && ((NewNode) this.alternateTree).isNullError() == false) {
                if (this.getErrorWidth() > 300 && ((NewNode) this.alternateTree).getErrorWidth() > 300) {
                    double oldErrorRate = this.getErrorEstimation();
                    double altErrorRate = ((NewNode) this.alternateTree).getErrorEstimation();
                    double fDelta = .05;
                    //if (gNumAlts>0) fDelta=fDelta/gNumAlts;
                    double fN = 1.0 / ((double) ((NewNode) this.alternateTree).getErrorWidth()) + 1.0 / ((double) this.getErrorWidth());
                    double Bound = (double) Math.sqrt((double) 2.0 * oldErrorRate * (1.0 - oldErrorRate) * Math.log(2.0 / fDelta) * fN);
                    if (Bound < oldErrorRate - altErrorRate) {
                        // Switch alternate tree
                        ht.activeLeafNodeCount -= this.numberLeaves();
                        ht.activeLeafNodeCount += ((NewNode) this.alternateTree).numberLeaves();
                        killTreeChilds(ht);
                        if (parent != null) {
                            parent.setChild(parentBranch, this.alternateTree);
                            //((AdaSplitNode) parent.getChild(parentBranch)).alternateTree = null;
                            System.out.print("Switched Sub-Tree with depth ");
                            System.out.println(subtreeDepth());
                        } else {
                            // Switch root tree
                            ht.treeRoot = ((AdaSplitNode) ht.treeRoot).alternateTree;
                            System.out.println("Switched Root");
                        }
                        ht.switchedAlternateTrees++;
                    } else if (Bound < altErrorRate - oldErrorRate) {
                        // Erase alternate tree
                        if (this.alternateTree instanceof ActiveLearningNode) {
                            this.alternateTree = null;
                            //ht.activeLeafNodeCount--;
                        } else if (this.alternateTree instanceof InactiveLearningNode) {
                            this.alternateTree = null;
                            //ht.inactiveLeafNodeCount--;
                        } else {
                            ((AdaSplitNode) this.alternateTree).killTreeChilds(ht);
                        }
                        ht.prunedAlternateTrees++;
                        System.out.print("Pruned Sub-Tree with depth ");
                        System.out.println(subtreeDepth());
                    }
                }
            }
            //}
            //learnFromInstance alternate Tree and Child nodes
            if (this.alternateTree != null) {
                ((NewNode) this.alternateTree).learnFromInstance(weightedInst, ht, parent, parentBranch);
            }
            int childBranch = this.instanceChildIndex(inst);
            Node child = this.getChild(childBranch);
            if (child != null) {
                ((NewNode) child).learnFromInstance(weightedInst, ht, this, childBranch);
            }
        }

        @Override
        public void killTreeChilds(HoeffdingAdaptiveTree ht) {
            for (Node child : this.children) {
                if (child != null) {
                    //Delete alternate tree if it exists
                    if (child instanceof AdaSplitNode && ((AdaSplitNode) child).alternateTree != null) {
                        ((NewNode) ((AdaSplitNode) child).alternateTree).killTreeChilds(ht);
                        ht.prunedAlternateTrees++;
                    }
                    //Recursive delete of SplitNodes
                    if (child instanceof AdaSplitNode) {
                        ((NewNode) child).killTreeChilds(ht);
                    }
                    if (child instanceof ActiveLearningNode) {
                        child = null;
                        ht.activeLeafNodeCount--;
                    } else if (child instanceof InactiveLearningNode) {
                        child = null;
                        ht.inactiveLeafNodeCount--;
                    }
                }
            }
        }

        //New for option votes
        //@Override
        public void filterInstanceToLeaves(Instance inst, SplitNode myparent,
                int parentBranch, List<FoundNode> foundNodes,
                boolean updateSplitterCounts) {
            if (updateSplitterCounts) {
                this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
            }
            int childIndex = instanceChildIndex(inst);
            if (childIndex >= 0) {
                Node child = getChild(childIndex);
                if (child != null) {
                    ((NewNode) child).filterInstanceToLeaves(inst, this, childIndex,
                            foundNodes, updateSplitterCounts);
                } else {
                    foundNodes.add(new FoundNode(null, this, childIndex));
                }
            }
            if (this.alternateTree != null) {
                ((NewNode) this.alternateTree).filterInstanceToLeaves(inst, this, -999,
                        foundNodes, updateSplitterCounts);
            }
        }
    }

    public static class AdaLearningNode extends LearningNodeNBAdaptive implements NewNode {

        private static final long serialVersionUID = 1L;

        protected ADWIN estimationErrorWeight;

        public boolean ErrorChange = false;

        protected int randomSeed = 1;

        protected Random classifierRandom;

        @Override
        public int calcByteSize() {
            int byteSize = super.calcByteSize();
            if (estimationErrorWeight != null) {
                byteSize += estimationErrorWeight.measureByteSize();
            }
            return byteSize;
        }

        public AdaLearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
            this.classifierRandom = new Random(this.randomSeed);
        }

        @Override
        public int numberLeaves() {
            return 1;
        }

        @Override
        public double getErrorEstimation() {
            if (this.estimationErrorWeight != null) {
                return this.estimationErrorWeight.getEstimation();
            } else {
                return 0;
            }
        }

        @Override
        public double getErrorWidth() {
            return this.estimationErrorWeight.getWidth();
        }

        @Override
        public boolean isNullError() {
            return (this.estimationErrorWeight == null);
        }

        @Override
        public void killTreeChilds(HoeffdingAdaptiveTree ht) {
        }

        @Override
        public void learnFromInstance(Instance inst, HoeffdingAdaptiveTree ht, SplitNode parent, int parentBranch) {
            int trueClass = (int) inst.classValue();
            //New option vore
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            Instance weightedInst = (Instance) inst.copy();
            if (k > 0) {
                weightedInst.setWeight(inst.weight() * k);
            }
            //Compute ClassPrediction using filterInstanceToLeaf
            int ClassPrediction = Utils.maxIndex(this.getClassVotes(inst, ht));

            boolean blCorrect = (trueClass == ClassPrediction);

            if (this.estimationErrorWeight == null) {
                this.estimationErrorWeight = new ADWIN();
            }
            double oldError = this.getErrorEstimation();
            this.ErrorChange = this.estimationErrorWeight.setInput(blCorrect == true ? 0.0 : 1.0);
            if (this.ErrorChange == true && oldError > this.getErrorEstimation()) {
                this.ErrorChange = false;
            }

            //Update statistics
            learnFromInstance(weightedInst, ht);	//inst

            //Check for Split condition
            double weightSeen = this.getWeightSeen();
            if (weightSeen
                    - this.getWeightSeenAtLastSplitEvaluation() >= ht.gracePeriodOption.getValue()) {
                ht.attemptToSplit(this, parent,
                        parentBranch);
                this.setWeightSeenAtLastSplitEvaluation(weightSeen);
            }


            //learnFromInstance alternate Tree and Child nodes
			/*if (this.alternateTree != null)  {
            this.alternateTree.learnFromInstance(inst,ht);
            }
            for (Node child : this.children) {
            if (child != null) {
            child.learnFromInstance(inst,ht);
            }
            }*/
        }

        @Override
        public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
            double[] dist;
            int predictionOption = ((HoeffdingAdaptiveTree) ht).leafpredictionOption.getChosenIndex();
            if (predictionOption == 0) { //MC
                dist = this.observedClassDistribution.getArrayCopy();
            } else if (predictionOption == 1) { //NB
                dist = NaiveBayes.doNaiveBayesPrediction(inst,
                        this.observedClassDistribution, this.attributeObservers);
            } else { //NBAdaptive
                if (this.mcCorrectWeight > this.nbCorrectWeight) {
                    dist = this.observedClassDistribution.getArrayCopy();
                } else {
                    dist = NaiveBayes.doNaiveBayesPrediction(inst,
                            this.observedClassDistribution, this.attributeObservers);
                }
            }
            //New for option votes
            double distSum = Utils.sum(dist);
            if (distSum * this.getErrorEstimation() * this.getErrorEstimation() > 0.0) {
                Utils.normalize(dist, distSum * this.getErrorEstimation() * this.getErrorEstimation()); //Adding weight
            }
            return dist;
        }

        //New for option votes
        @Override
        public void filterInstanceToLeaves(Instance inst,
                SplitNode splitparent, int parentBranch,
                List<FoundNode> foundNodes, boolean updateSplitterCounts) {
            foundNodes.add(new FoundNode(this, splitparent, parentBranch));
        }
    }

    protected int alternateTrees;

    protected int prunedAlternateTrees;

    protected int switchedAlternateTrees;

    public int getSwitchedAlternateTrees(){
        return this.switchedAlternateTrees;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{
                new Measurement("tree size (nodes)", this.decisionNodeCount
                        + this.activeLeafNodeCount + this.inactiveLeafNodeCount),
                new Measurement("tree size (leaves)", this.activeLeafNodeCount
                        + this.inactiveLeafNodeCount),
                new Measurement("active learning leaves",
                        this.activeLeafNodeCount),
                new Measurement("tree depth", measureTreeDepth()),
                new Measurement("active leaf byte size estimate",
                        this.activeLeafByteSizeEstimate),
                new Measurement("inactive leaf byte size estimate",
                        this.inactiveLeafByteSizeEstimate),
                new Measurement("byte size estimate overhead",
                        this.byteSizeEstimateOverheadFraction),
                new Measurement("switched alternate trees",
                        this.switchedAlternateTrees),
                new Measurement("pruned alternate trees",
                        this.prunedAlternateTrees)};

    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        // IDEA: to choose different learning nodes depending on predictionOption
        return new AdaLearningNode(initialClassObservations);
    }

   @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
            double[] classObservations, int size) {
        return new AdaSplitNode(splitTest, classObservations, size);
    }
   
    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
            double[] classObservations) {
        return new AdaSplitNode(splitTest, classObservations);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            this.activeLeafNodeCount = 1;
        }
        ((NewNode) this.treeRoot).learnFromInstance(inst, this, null, -1);
        instanceCount++;
    }

    //New for options vote
    public FoundNode[] filterInstanceToLeaves(Instance inst,
            SplitNode parent, int parentBranch, boolean updateSplitterCounts) {
        List<FoundNode> nodes = new LinkedList<FoundNode>();
        ((NewNode) this.treeRoot).filterInstanceToLeaves(inst, parent, parentBranch, nodes,
                updateSplitterCounts);
        return nodes.toArray(new FoundNode[nodes.size()]);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.treeRoot != null) {
            FoundNode[] foundNodes = filterInstanceToLeaves(inst,
                    null, -1, false);
            DoubleVector result = new DoubleVector();
            int predictionPaths = 0;
            for (FoundNode foundNode : foundNodes) {
                if (foundNode.parentBranch != -999) {
                    Node leafNode = foundNode.node;
                    if (leafNode == null) {
                        leafNode = foundNode.parent;
                    }
                    double[] dist = leafNode.getClassVotes(inst, this);
                    //Albert: changed for weights
                    //double distSum = Utils.sum(dist);
                    //if (distSum > 0.0) {
                    //	Utils.normalize(dist, distSum);
                    //}
                    result.addValues(dist);
                    //predictionPaths++;
                }
            }
            //if (predictionPaths > this.maxPredictionPaths) {
            //	this.maxPredictionPaths++;
            //}
            return result.getArrayRef();
        }
        return new double[0];
    }
}
