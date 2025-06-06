# 🌐 Semantic Web & GraphDB

An application built with **Java** and **RDF4J** that transforms **RDFS** models into **OWL**, loads data into a remote **GraphDB**, and runs **SPARQL** queries to extract statistics.

---

## 📌 Features  
✔ Conversion from **RDF Schema (RDFS)** to **Web Ontology Language (OWL)**  
✔ Ontology loading into **GraphDB** via **RDF4J**  
✔ Creation and execution of **SPARQL** queries  
✔ Analysis and extraction of **statistical data** from the model  

---

## 🛠 Technologies  
🔹 **Java 17**  
🔹 **Maven**  
🔹 **RDF4J**  
🔹 **GraphDB**  
🔹 **SPARQL**  

---

## 📂 Files  

- `Main.java` → **Main class** of the application  
- `astronomy.ttl` → **Initial RDFS file** containing astronomy-related data  
- `astronomyOWL.ttl` → **Transformed OWL** version of the ontology  
- `pom.xml` → **Maven dependencies and build configuration**  
- `report_projectB_SemanticWeb.pdf` → **Project report**  

---

## 🚀 How to Run  

1️⃣ **Ensure GraphDB is running locally** at [`http://localhost:7200/`](http://localhost:7200/)  
2️⃣ **Edit** the `Main.java` file to set the correct **repository name**  
3️⃣ **Run the application** with the following command:  

```sh
mvn compile exec:java
