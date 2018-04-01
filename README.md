# Concept-Drift-Detector-Selection-For-Hoeffding-Adaptive-Trees
Extensions to MOA for additional stream generators and classifiers used as part of "Concept Drift Detector Selection For Hoeffding Adaptive Trees". Most of these are modifications of existing MOA classes to work for the specfic purposes of the research.

Classes are usable as part of the [MOA framework](https://moa.cms.waikato.ac.nz/).

Classifiers included are:
- Hoeffding Adatpive Tree using SEED concept drift detector (HAT-SEED)
- Hoeffding Adatpive Tree using HDDM_A concept drift detector (HAT-HDDMA)
- Hoeffding Adatpive Tree using Page-Hinckley Test for concept drift detection (HAT-PHT)

Concept drift detectors included are:
- SEED concept drift detector
- HDDM_A concept drift detector
- Page-Hinckley Test for concept drift detection

Data stream generators included are:
- SEA drift generator
- Agrawal drift generator
- RBF drift generator
- Abrupt Hyperplane drift generator
- Gradual hyperplane generator
