import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {

        String repositoryName = "ProjectB";
        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/" + repositoryName);

        File rdfsFile = new File("src/main/resources/astronomy.ttl");
        InputStream inputStream = new FileInputStream(rdfsFile);

        try (RepositoryConnection connection = repository.getConnection()) {
            // connecting to GraphDB repository
            connection.clear();
            connection.begin();
            connection.add(inputStream, "", RDFFormat.TURTLE);
            connection.commit();

            Model owlModel = new TreeModel();
            owlModel.setNamespace("owl", "http://www.w3.org/2002/07/owl#");
            owlModel.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            owlModel.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            owlModel.setNamespace("astronomy", "http://example.org/astronomy#");
            owlModel.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");

            //ΜΕΤΑΣΧΗΜΑΤΙΣΜΟΣ ΑΠΟ RDFS ΣΕ OWL

            // Copying all the statements of the rdfs file to the new model
            connection.getStatements(null, null, null).forEach(statement -> {
                owlModel.add(statement.getSubject(), statement.getPredicate(), statement.getObject());
            });

            // replacing the statements that are of type rdfs:Class with owl:Class
            connection.getStatements(null, RDF.TYPE, RDFS.CLASS).forEach(statement -> {
                IRI classIRI = (IRI) statement.getSubject();
                owlModel.remove(classIRI, RDF.TYPE, RDFS.CLASS);
                owlModel.add(classIRI, RDF.TYPE, OWL.CLASS);
            });

            // replacing the statements that are of type rdf:property with owl:DataTypeProperty
            // or owl:ObjectTypeProperty, depending on what their range's type is.
            // if a property does not have range, then we set it to owl:ObjectProperty.
            connection.getStatements(null, RDF.TYPE, RDF.PROPERTY).forEach(statement -> {
                IRI propertyIRI = (IRI) statement.getSubject();

                boolean hasRange = connection.getStatements(propertyIRI, RDFS.RANGE, null).hasNext();
                if (hasRange) {
                    connection.getStatements(propertyIRI, RDFS.RANGE, null).forEach(rangeStmt -> {
                        IRI rangeIRI = (IRI) rangeStmt.getObject();
                        if (rangeIRI.getNamespace().equals("http://www.w3.org/2001/XMLSchema#")) {
                            owlModel.remove(propertyIRI, RDF.TYPE, RDF.PROPERTY);
                            owlModel.add(propertyIRI, RDF.TYPE, OWL.DATATYPEPROPERTY);
                        }
                        else {
                            owlModel.remove(propertyIRI, RDF.TYPE, RDF.PROPERTY);
                            owlModel.add(propertyIRI, RDF.TYPE, OWL.OBJECTPROPERTY);
                        }
                    });
                }
                else {
                    owlModel.remove(propertyIRI, RDF.TYPE, RDF.PROPERTY);
                    owlModel.add(propertyIRI, RDF.TYPE, OWL.OBJECTPROPERTY);
                }
            });

            //ΟΡΙΣΜΟΣ ΠΕΡΙΟΡΙΣΜΩΝ

            IRI Star = Values.iri("http://example.org/astronomy#Star");
            IRI Planet = Values.iri("http://example.org/astronomy#Planet");
            IRI influences = Values.iri("http://example.org/astronomy#influences");
            IRI StellarObject = Values.iri("http://example.org/astronomy#StellarObject");
            IRI Moon = Values.iri("http://example.org/astronomy#Moon");
            IRI surroundedBy = Values.iri("http://example.org/astronomy#surroundedBy");
            IRI PlanetaryObject = Values.iri("http://example.org/astronomy#PlanetaryObject");
            IRI orbitsAroundStar = Values.iri("http://example.org/astronomy#orbitsAroundStar");
            IRI MainSequenceStar = Values.iri("http://example.org/astronomy#MainSequenceStar");
            IRI Comet = Values.iri("http://example.org/astronomy#Comet");
            IRI collidedWith = Values.iri("http://example.org/astronomy#collidedWith");
            IRI Sun = Values.iri("http://example.org/astronomy#Sun");
            IRI MilkyWay = Values.iri("http://example.org/astronomy#MilkyWay");
            IRI partOfGalaxy = Values.iri("http://example.org/astronomy#partOfGalaxy");

            //owl:someValuesFrom
            BNode restriction1 = Values.bnode();
            owlModel.add(Star, RDFS.SUBCLASSOF, restriction1);
            owlModel.add(restriction1, RDF.TYPE, OWL.RESTRICTION);
            owlModel.add(restriction1, OWL.ONPROPERTY, influences);
            owlModel.add(restriction1, OWL.SOMEVALUESFROM, Planet);

            BNode restriction2 = Values.bnode();
            owlModel.add(StellarObject, RDFS.SUBCLASSOF, restriction2);
            owlModel.add(restriction2, RDF.TYPE, OWL.RESTRICTION);
            owlModel.add(restriction2, OWL.ONPROPERTY, surroundedBy);
            owlModel.add(restriction2, OWL.SOMEVALUESFROM, Moon);

            //owl:allValuesFrom
            BNode restriction3 = Values.bnode();
            owlModel.add(PlanetaryObject, RDFS.SUBCLASSOF, restriction3);
            owlModel.add(restriction3, RDF.TYPE, OWL.RESTRICTION);
            owlModel.add(restriction3, OWL.ONPROPERTY, orbitsAroundStar);
            owlModel.add(restriction3, OWL.ALLVALUESFROM, MainSequenceStar);

            BNode restriction4 = Values.bnode();
            owlModel.add(Planet, RDFS.SUBCLASSOF, restriction4);
            owlModel.add(restriction4, RDF.TYPE, OWL.RESTRICTION);
            owlModel.add(restriction4, OWL.ONPROPERTY, collidedWith);
            owlModel.add(restriction4, OWL.ALLVALUESFROM, Comet);

            //owl:hasValue
            BNode restriction5 = Values.bnode();
            owlModel.add(Planet, RDFS.SUBCLASSOF, restriction5);
            owlModel.add(restriction5, RDF.TYPE, OWL.RESTRICTION);
            owlModel.add(restriction5, OWL.ONPROPERTY, orbitsAroundStar);
            owlModel.add(restriction5, OWL.HASVALUE, Sun);

            BNode restriction6 = Values.bnode();
            owlModel.add(Moon, RDFS.SUBCLASSOF, restriction6);
            owlModel.add(restriction6, RDF.TYPE, OWL.RESTRICTION);
            owlModel.add(restriction6, OWL.ONPROPERTY, partOfGalaxy);
            owlModel.add(restriction6, OWL.HASVALUE, MilkyWay);

            // ΤΥΠΟΙ ΙΔΙΟΤΗΤΩΝ

            IRI connectedTo = Values.iri("http://example.org/astronomy#connectedTo");
            IRI orbitsAround = Values.iri("http://example.org/astronomy#orbitsAround");
            IRI illuminatedBy = Values.iri("http://example.org/astronomy#illuminatedBy");
            IRI hasArtificialSatellite = Values.iri("http://example.org/astronomy#hasArtificialSatellite");
            IRI illuminates = Values.iri("http://example.org/astronomy#illuminates");
            IRI artificialSatelliteOf = Values.iri("http://example.org/astronomy#artificialSatelliteOf");

            //owl:SymmetricProperty
            owlModel.add(connectedTo, RDF.TYPE, OWL.SYMMETRICPROPERTY);
            owlModel.add(collidedWith, RDF.TYPE, OWL.SYMMETRICPROPERTY);

            //owl:TransitiveProperty
            owlModel.add(influences, RDF.TYPE, OWL.TRANSITIVEPROPERTY);
            owlModel.add(orbitsAround, RDF.TYPE, OWL.TRANSITIVEPROPERTY);

            //owl:inverseOf
            owlModel.add(illuminatedBy, OWL.INVERSEOF, illuminates);
            owlModel.add(hasArtificialSatellite, OWL.INVERSEOF, artificialSatelliteOf);

            // ΠΡΟΣΘΗΚΗ ΝΕΩΝ ΑΝΤΙΚΕΙΜΕΝΩΝ
            objectAddition(owlModel);

            // ΦΟΡΤΩΣΗ ΤΟΥ ΜΟΝΤΕΛΟΥ ΣΤΟ GRAPHDB
            connection.clear();
            connection.begin();
            connection.add(owlModel);
            connection.commit();

            // saving the new owl model to a new file
            File outputFile = new File("src/main/resources/astronomyOWL.ttl");
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                Rio.write(owlModel, out, RDFFormat.TURTLE);
            }

            // ΔΗΜΙΟΥΡΓΙΑ ΚΑΙ ΕΚΤΕΛΕΣΗ SPARQL ΕΡΩΤΗΜΑΤΩΝ
            sparqlQueries(connection);

            // ΕΞΑΓΩΓΗ ΣΤΑΤΙΣΤΙΚΩΝ ΔΕΔΟΜΕΝΩΝ
            statistics(owlModel);
        } finally {
            repository.shutDown();
        }
    }

    private static void statistics(Model owlModel) {
        Map<String, Integer> propertyFreq = new HashMap<>();
        Map<String, Integer> numOfClassObjects = new HashMap<>();
        Map<String, Integer> subjectConnections = new HashMap<>();
        Map<String, Integer> usesOfDatatypeProperties = new HashMap<>();
        Map<String, Integer> usesOfCertainClasses = new HashMap<>();
        Integer count = 1;
        Integer numOfTriplets = 0;

        for (Statement statement : owlModel) {
            String predicate = statement.getPredicate().getLocalName();

            // ΣΥΧΝΟΤΗΤΑ ΕΜΦΑΝΙΣΗΣ ΣΥΓΚΕΚΡΙΜΕΝΩΝ ΙΔΙΟΤΗΤΩΝ
            propertyFreq.put(predicate, propertyFreq.getOrDefault(predicate, 0) + 1);

            // ΚΑΤΑΝΟΜΗ ΤΥΠΩΝ ΑΝΤΙΚΕΙΜΕΝΩΝ
            if (statement.getPredicate().equals(RDF.TYPE)) {
                String objectIRI = statement.getObject().toString();
                String object = objectIRI.substring(objectIRI.lastIndexOf('#') + 1);

                numOfClassObjects.put(object, numOfClassObjects.getOrDefault(object, 0) + 1);
            }

            // ΜΕΣΟΣ ΑΡΙΘΜΟΣ ΣΥΝΔΕΣΕΩΝ ΑΝΑ ΑΝΤΙΚΕΙΜΕΝΟ
            String subjectIRI = statement.getSubject().toString();
            String subject = subjectIRI.substring(subjectIRI.lastIndexOf('#') + 1);

            subjectConnections.put(subject, subjectConnections.getOrDefault(subject, 0) + 1);

            // ΣΥΝΟΛΙΚΟΣ ΑΡΙΘΜΟΣ ΤΡΙΠΛΕΤΩΝ
            numOfTriplets += count;

            // ΚΑΤΑΝΟΜΗ ΤΙΜΩΝ ΓΙΑ ΙΔΙΟΤΗΤΕΣ
            if (statement.getObject() instanceof Literal) {
                Literal datatypeObjLit = (Literal) statement.getObject();
                IRI datatypeIRI = datatypeObjLit.getDatatype();
                if (datatypeIRI != null) {
                    String datatypeObj = datatypeIRI.getLocalName();
                    usesOfDatatypeProperties.put(datatypeObj, usesOfDatatypeProperties.getOrDefault(datatypeObj, 0) + 1);
                }
            }

            // ΣΥΧΝΟΤΗΤΑ ΧΡΗΣΗΣ ΣΥΓΚΕΚΡΙΜΕΝΩΝ ΚΛΑΣΕΩΝ
            IRI pred = statement.getPredicate();
            Value obj = statement.getObject();
            String class_ = obj.stringValue();
            String class_name = class_.substring(class_.lastIndexOf('#') + 1);

            if (obj instanceof IRI) {
                if (!(((IRI) obj).getNamespace().equals("http://www.w3.org/2001/XMLSchema#"))) {
                    if (pred.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                    else if (pred.toString().equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                    else if (pred.toString().equals("http://www.w3.org/2000/01/rdf-schema#range")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                    else if (pred.toString().equals("http://www.w3.org/2000/01/rdf-schema#domain")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                    else if (pred.toString().equals("http://www.w3.org/2002/07/owl#someValuesFrom")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                    else if (pred.toString().equals("http://www.w3.org/2002/07/owl#allValuesFrom")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                    else if (pred.toString().equals("http://www.w3.org/2002/07/owl#hasValue")) {
                        usesOfCertainClasses.put(class_name, usesOfCertainClasses.getOrDefault(class_name, 0) + 1);
                    }
                }
            }
        }

        double numOfSubjects = subjectConnections.size();
        Integer sumOfConnections = 0;
        for (Integer connection : subjectConnections.values()) {
            sumOfConnections += connection;
        }
        double connection_avg = numOfSubjects > 0 ? sumOfConnections / numOfSubjects : 0;

        // Results
        System.out.println("Frequency of each property:");
        System.out.printf("%-30s | %-30s\n", "Property", "Frequency");
        System.out.println("-------------------------------------------------");
        for (Map.Entry<String, Integer> entry : propertyFreq.entrySet()) {
            System.out.printf("%-30s | %-30d\n", entry.getKey(), entry.getValue());
        }
        System.out.println("\n");
        System.out.println("Number of objects that belong to each class:");
        System.out.printf("%-30s | %-30s\n", "Class", "# of Objects");
        System.out.println("-------------------------------------------------");
        for (Map.Entry<String, Integer> entry : numOfClassObjects.entrySet()) {
            System.out.printf("%-30s | %-30d\n", entry.getKey(), entry.getValue());
        }
        System.out.println("\n");
        System.out.println(String.format("Average number of connections per object: %.2f\n", connection_avg));
        System.out.println("Total number of triplets: " + numOfTriplets + "\n");
        System.out.println("Value Distribution for properties:");
        System.out.printf("%-15s | %-15s\n", "Value type", "# of Properties that use them");
        System.out.println("-------------------------------------------------");
        for (Map.Entry<String, Integer> entry : usesOfDatatypeProperties.entrySet()) {
            System.out.printf("%-15s | %-15d\n", entry.getKey(), entry.getValue());
        }
        System.out.println("\n");
        System.out.println("Usage Frequency of certain Classes");
        System.out.printf("%-30s | %-15s\n", "Class", "Frequency");
        System.out.println("-------------------------------------------------");
        for (Map.Entry<String, Integer> entry : usesOfCertainClasses.entrySet()) {
            System.out.printf("%-30s | %-15d\n", entry.getKey(), entry.getValue());
        }
    }

    private static void sparqlQueries(RepositoryConnection connection) {
        // Query 1
        String queryString = "PREFIX astronomy: <http://example.org/astronomy#> \n";
        queryString += "SELECT ?planet (COUNT(?moon) AS ?moonCount) ?star \n";
        queryString += "WHERE { \n";
        queryString += "?planet a astronomy:Planet ; \n";
        queryString += "astronomy:orbitsAroundStar ?star . \n";
        queryString += "?moon astronomy:orbitsAroundPlanet ?planet . \n";
        queryString += "} \n";
        queryString += "GROUP BY ?planet ?star \n";
        queryString += "HAVING (?moonCount > 1) \n";
        queryString += "ORDER BY DESC(?moonCount) \n";

        TupleQuery query1 = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query1.evaluate()) {
            System.out.println("QUERY #1");
            System.out.printf("%-10s | %-10s | %-10s\n", "Planet", "Moon Count", "Star");
            System.out.println("--------------------------------");
            for (BindingSet solution : result) {
                String planetIRI = solution.getValue("planet").stringValue();
                String planet = planetIRI.substring(planetIRI.lastIndexOf('#') + 1);
                int moonCount = Integer.parseInt(solution.getValue("moonCount").stringValue());
                String starIRI = solution.getValue("star").stringValue();
                String star = starIRI.substring(starIRI.lastIndexOf('#') + 1);

                System.out.printf("%-10s | %-10d | %-10s\n", planet, moonCount, star);
            }
            System.out.println("\n");
        }

        // Query 2
        queryString = "PREFIX astronomy: <http://example.org/astronomy#> \n";
        queryString += "SELECT ?astrObject ?artSat \n";
        queryString += "WHERE { \n";
        queryString += "?artSat a astronomy:ArtificialSatellite. \n";
        queryString += "?astrObject a astronomy:AstronomicalObject. \n";
        queryString += "?artSat astronomy:artificialSatelliteOf ?astrObject.";
        queryString += "} \n";
        queryString += "ORDER BY (?artSat) \n";

        TupleQuery query2 = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query2.evaluate()) {
            System.out.println("QUERY #2");
            System.out.printf("%-20s | %-20s\n", "Astronomical Object", "Artificial Satellite");
            System.out.println("----------------------------------------------");
            for (BindingSet solution: result) {
                String astrObjectIRI = solution.getValue("astrObject").stringValue();
                String astrObject = astrObjectIRI.substring(astrObjectIRI.lastIndexOf('#') + 1);
                String artSatIRI = solution.getValue("artSat").stringValue();
                String artSat = artSatIRI.substring(artSatIRI.lastIndexOf('#') + 1);

                System.out.printf("%-20s | %-20s\n", astrObject, artSat);
            }
            System.out.println("\n");
        }

        // Query 3
        queryString = "PREFIX astronomy: <http://example.org/astronomy#> \n";
        queryString += "SELECT ?object ?type \n";
        queryString += "WHERE { \n";
        queryString += "?stellarObj a astronomy:StellarObject; \n";
        queryString += "astronomy:surroundedBy ?object. \n";
        queryString += "?object a ?type. \n";
        queryString += "FILTER (?type = astronomy:Moon || ?type = astronomy:Planet) \n";
        queryString += "} \n";
        queryString += "LIMIT 10 \n";

        TupleQuery query3 = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query3.evaluate()) {
            System.out.println("QUERY #3");
            System.out.printf("%-10s | %-10s\n", "Object", "Type Of Object");
            System.out.println("----------------------------------------------");
            for (BindingSet solution: result) {
                String objectIRI = solution.getValue("object").stringValue();
                String object = objectIRI.substring(objectIRI.lastIndexOf('#') + 1);
                String typeIRI = solution.getValue("type").stringValue();
                String type = typeIRI.substring(typeIRI.lastIndexOf('#') + 1);

                System.out.printf("%-10s | %-10s\n", object, type);
            }
            System.out.println("\n");
        }

        // Query 4
        queryString = "PREFIX astronomy: <http://example.org/astronomy#> \n";
        queryString += "SELECT ?subject ?galaxy \n";
        queryString += "WHERE { \n";
        queryString += "?subject a astronomy:Moon; \n";
        queryString += "astronomy:partOfGalaxy ?galaxy; \n";
        queryString += "astronomy:observedByTelescope ?telescope. \n";
        queryString += "FILTER(?telescope = astronomy:HubbleSpaceTelescope) \n";
        queryString += "} \n";
        queryString += "ORDER BY DESC(?subject) \n";

        TupleQuery query4 = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query4.evaluate()) {
            System.out.println("QUERY #4");
            System.out.printf("%-10s | %-10s\n", "Subject", "Galaxy");
            System.out.println("-----------------------------------");
            for (BindingSet solution: result) {
                String subjectIRI = solution.getValue("subject").stringValue();
                String subject = subjectIRI.substring(subjectIRI.lastIndexOf('#') + 1);
                String galaxyIRI = solution.getValue("galaxy").stringValue();
                String galaxy = galaxyIRI.substring(galaxyIRI.lastIndexOf('#') + 1);

                System.out.printf("%-10s | %-10s\n", subject, galaxy);
            }
            System.out.println("\n");
        }

        // Query 5
        queryString = "PREFIX astronomy: <http://example.org/astronomy#> \n";
        queryString += "SELECT ?subject ?object \n";
        queryString += "WHERE { \n";
        queryString += "?subject a ?type ; \n";
        queryString += "astronomy:influences ?object. \n";
        queryString += "FILTER(?type = astronomy:Planet || ?type = astronomy:Moon) \n";
        queryString += "} \n";
        queryString += "ORDER BY(?subject)";

        TupleQuery query5 = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query5.evaluate()) {
            System.out.println("QUERY #5");
            System.out.printf("%-10s | %-10s\n", "Subject", "Object");
            System.out.println("-----------------------------------");
            for (BindingSet solution: result) {
                String subjectIRI = solution.getValue("subject").stringValue();
                String subject = subjectIRI.substring(subjectIRI.lastIndexOf('#') + 1);
                String objectIRI = solution.getValue("object").stringValue();
                String object = objectIRI.substring(objectIRI.lastIndexOf('#') + 1);

                System.out.printf("%-10s | %-10s\n", subject, object);
            }
            System.out.println("\n");
        }
    }

    private static void objectAddition(Model owlModel) {
        // 1η ΚΛΑΣΗ: UnmannedSpacecraft
        IRI UnmannedSpacecraft = Values.iri("http://example.org/astronomy#UnmannedSpacecraft");
        IRI Juno = Values.iri("http://example.org/astronomy#Juno");
        owlModel.add(Juno, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(Juno, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(true));
        owlModel.add(Juno, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(Juno, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("2011-08-05", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Juno, RDFS.LABEL, Values.literal("Juno"));

        IRI Viking = Values.iri("http://example.org/astronomy#Viking");
        owlModel.add(Viking, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(Viking, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(false));
        owlModel.add(Viking, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(Viking, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("1975-08-20", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Viking, RDFS.LABEL, Values.literal("Viking"));

        IRI MagellanSpacecraft = Values.iri("http://example.org/astronomy#MagellanSpacecraft");
        owlModel.add(MagellanSpacecraft, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(MagellanSpacecraft, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(false));
        owlModel.add(MagellanSpacecraft, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(MagellanSpacecraft, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("1989-05-04", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(MagellanSpacecraft, RDFS.LABEL, Values.literal("MagellanSpacecraft"));

        IRI Giotto = Values.iri("http://example.org/astronomy#Giotto");
        owlModel.add(Giotto, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(Giotto, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(false));
        owlModel.add(Giotto, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("ESA"));
        owlModel.add(Giotto, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("1985-07-02", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Giotto, RDFS.LABEL, Values.literal("Giotto"));

        IRI DeepImpact = Values.iri("http://example.org/astronomy#DeepImpact");
        owlModel.add(DeepImpact, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(DeepImpact, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(false));
        owlModel.add(DeepImpact, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(DeepImpact, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("2005-01-12", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(DeepImpact, RDFS.LABEL, Values.literal("DeepImpact"));

        IRI NewHorizons = Values.iri("http://example.org/astronomy#NewHorizons");
        owlModel.add(NewHorizons, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(NewHorizons, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(true));
        owlModel.add(NewHorizons, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(NewHorizons, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("2006-01-19", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(NewHorizons, RDFS.LABEL, Values.literal("NewHorizons"));

        IRI VoyagerProbes = Values.iri("http://example.org/astronomy#VoyagerProbes");
        owlModel.add(VoyagerProbes, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(VoyagerProbes, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(true));
        owlModel.add(VoyagerProbes, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(VoyagerProbes, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("1977-09-05", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(VoyagerProbes, RDFS.LABEL, Values.literal("VoyagerProbes"));

        IRI H_IIA202 = Values.iri("http://example.org/astronomy#H_IIA202");
        owlModel.add(H_IIA202, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(H_IIA202, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(true));
        owlModel.add(H_IIA202, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("Mitsubishi Heavy Industries"));
        owlModel.add(H_IIA202, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("2003-02-04", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(H_IIA202, RDFS.LABEL, Values.literal("H_IIA202"));

        IRI MESSENGERSpacecraft = Values.iri("http://example.org/astronomy#MESSENGERSpacecraft");
        owlModel.add(MESSENGERSpacecraft, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(MESSENGERSpacecraft, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(true));
        owlModel.add(MESSENGERSpacecraft, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("NASA"));
        owlModel.add(MESSENGERSpacecraft, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("2004-08-03", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(MESSENGERSpacecraft, RDFS.LABEL, Values.literal("MESSENGER"));

        IRI MarsExpress = Values.iri("http://example.org/astronomy#MarsExpress");
        owlModel.add(MarsExpress, RDF.TYPE, UnmannedSpacecraft);
        owlModel.add(MarsExpress, Values.iri("http://example.org/astronomy#hasActiveStatus"), Values.literal(true));
        owlModel.add(MarsExpress, Values.iri("http://example.org/astronomy#hasOperator"), Values.literal("ESA"));
        owlModel.add(MarsExpress, Values.iri("http://example.org/astronomy#launchDate"), Values.literal("2003-06-02", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(MarsExpress, RDFS.LABEL, Values.literal("MarsExpress"));

        // extra declarations
        owlModel.add(Values.iri("http://example.org/astronomy#Jupiter"), Values.iri("http://example.org/astronomy#visitedBy"), Juno);
        owlModel.add(Values.iri("http://example.org/astronomy#Mars"), Values.iri("http://example.org/astronomy#visitedBy"), Viking);
        owlModel.add(Values.iri("http://example.org/astronomy#Venus"), Values.iri("http://example.org/astronomy#visitedBy"), MagellanSpacecraft);
        owlModel.add(Values.iri("http://example.org/astronomy#Halley"), Values.iri("http://example.org/astronomy#visitedBy"), Giotto);
        owlModel.add(Values.iri("http://example.org/astronomy#Tempel1"), Values.iri("http://example.org/astronomy#visitedBy"), DeepImpact);
        owlModel.add(Values.iri("http://example.org/astronomy#Pluto"), Values.iri("http://example.org/astronomy#visitedBy"), NewHorizons);
        owlModel.add(Values.iri("http://example.org/astronomy#Earth"), Values.iri("http://example.org/astronomy#visitedBy"), VoyagerProbes);
        owlModel.add(Values.iri("http://example.org/astronomy#Akatsuki"), Values.iri("http://example.org/astronomy#launchedBy"), H_IIA202);
        owlModel.add(Values.iri("http://example.org/astronomy#Mercury"), Values.iri("http://example.org/astronomy#visitedBy"), MESSENGERSpacecraft);
        owlModel.add(Values.iri("http://example.org/astronomy#Phobos"), Values.iri("http://example.org/astronomy#visitedBy"), MarsExpress);

        // 2η ΚΛΑΣΗ: Moon
        IRI Moon = Values.iri("http://example.org/astronomy#Moon");
        IRI EarthMoon = Values.iri("http://example.org/astronomy#EarthMoon");
        owlModel.add(EarthMoon, RDF.TYPE, Moon);
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Galileo Galilei"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1610-01-01", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("3474.8"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("1.62"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(true));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("10.8 trillion metric tons"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("0.0629"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("127.0"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#LunarReconnaissanceOrbiter"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(EarthMoon, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#Apollo"));
        owlModel.add(EarthMoon, RDFS.LABEL, Values.literal("Moon"));

        IRI Enceladus = Values.iri("http://example.org/astronomy#Enceladus");
        owlModel.add(Enceladus, RDF.TYPE, Moon);
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("William Herschel"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1789-08-28", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("504.2"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("0.113"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(true));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("0.108 quintillion metric tons"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("1.37"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-201.0"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Saturn"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Saturn"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Enceladus, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#Cassini-Huygens_Mission"));
        owlModel.add(Enceladus, RDFS.LABEL, Values.literal("Enceladus"));

        IRI Europa = Values.iri("http://example.org/astronomy#Europa");
        owlModel.add(Europa, RDF.TYPE, Moon);
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Galileo Galilei"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1610-01-08", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("3121.6"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("1.314"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(true));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("48 quintillion metric tons"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("0.00957"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-160.0"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Europa, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#GalileoSpacecraft"));
        owlModel.add(Europa, RDFS.LABEL, Values.literal("Europa"));

        IRI Ganymede = Values.iri("http://example.org/astronomy#Ganymede");
        owlModel.add(Ganymede, RDF.TYPE, Moon);
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Galileo Galilei"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1610-01-07", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("5268.2"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("1.428"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(true));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("148 quintillion metric tons"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("7.15"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-121.0"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Ganymede, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#GalileoSpacecraft"));
        owlModel.add(Ganymede, RDFS.LABEL, Values.literal("Ganymede"));

        IRI Miranda = Values.iri("http://example.org/astronomy#Miranda");
        owlModel.add(Miranda, RDF.TYPE, Moon);
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Gerard Kuiper"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1948-02-16", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("471.6"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("0.079"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(false));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("65.9 million trillion kilograms"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("1.413"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-187.0"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Uranus"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#observedBySpacecraft"), Values.iri("http://example.org/astronomy#Voyager2"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Uranus"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Miranda, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#Voyager2"));
        owlModel.add(Miranda, RDFS.LABEL, Values.literal("Miranda"));

        IRI Phobos = Values.iri("http://example.org/astronomy#Phobos");
        owlModel.add(Phobos, RDF.TYPE, Moon);
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Asaph Hall"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1877-08-18", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("22.4"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("0.0057"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(false));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("10.8 trillion metric tons"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("0.3"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-4.0"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Mars"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#MarsReconnaissanceOrbite"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Mars"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#Mariner2"));
        owlModel.add(Phobos, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#MarsExpress"));
        owlModel.add(Phobos, RDFS.LABEL, Values.literal("Phobos"));

        IRI Titan = Values.iri("http://example.org/astronomy#Titan");
        owlModel.add(Titan, RDF.TYPE, Moon);
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Christiaan Huygens"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1655-03-25", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("5151.8"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("1.352"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(true));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("134.5 quintillion metric tons"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("16"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-179.0"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Saturn"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Saturn"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Titan, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#Cassini-Huygens_Mission"));
        owlModel.add(Titan, RDFS.LABEL, Values.literal("Titan"));

        IRI Triton = Values.iri("http://example.org/astronomy#Triton");
        owlModel.add(Triton, RDF.TYPE, Moon);
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("William Lassell"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1846-10-10", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("2706.8"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("0.779"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(false));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("21,400,000 trillion kilograms"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("5.87685"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-235.0"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Neptune"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#observedBySpacecraft"), Values.iri("http://example.org/astronomy#Voyager2"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Neptune"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Triton, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#Voyager2"));
        owlModel.add(Triton, RDFS.LABEL, Values.literal("Triton"));

        IRI Callisto = Values.iri("http://example.org/astronomy#Callisto");
        owlModel.add(Callisto, RDF.TYPE, Moon);
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Galileo Galilei"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1610-01-07", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("4.821"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("1.235"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(false));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("107.6 quintillion metric tons"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("16.69"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("-139.0"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#observedBySpacecraft"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Callisto, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#GalileoSpacecraft"));
        owlModel.add(Callisto, RDFS.LABEL, Values.literal("Callisto"));

        IRI Io = Values.iri("http://example.org/astronomy#Io");
        owlModel.add(Io, RDF.TYPE, Moon);
        owlModel.add(Io, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Galileo Galilei"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1610-01-07", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("3643.2"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("1.796"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#hasLife"), Values.literal(false));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("89.3 quintillion metric tons"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#hasOrbitalPeriod"), Values.literal("1.769"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("1000"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#illuminatedBy"), Values.iri("http://example.org/astronomy#Sun"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#observedBySpacecraft"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#orbitsAroundPlanet"), Values.iri("http://example.org/astronomy#Jupiter"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#visibleFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Io, Values.iri("http://example.org/astronomy#visitedBy"), Values.iri("http://example.org/astronomy#GalileoSpacecraft"));
        owlModel.add(Io, RDFS.LABEL, Values.literal("Callisto"));

        // extra declarations
        owlModel.add(Values.iri("http://example.org/astronomy#theGreatAmerican"), Values.iri("http://example.org/astronomy#causedBy"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#theHarvestMoon"), Values.iri("http://example.org/astronomy#causedBy"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#theRingOfFire"), Values.iri("http://example.org/astronomy#causedBy"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#theSnowMoon"), Values.iri("http://example.org/astronomy#causedBy"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#Earth"), Values.iri("http://example.org/astronomy#influences"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#Earth"), Values.iri("http://example.org/astronomy#hasSatellite"), EarthMoon);
        owlModel.add(Values.iri("http://example.org/astronomy#Saturn"), Values.iri("http://example.org/astronomy#hasSatellite"), Enceladus);
        owlModel.add(Values.iri("http://example.org/astronomy#Saturn"), Values.iri("http://example.org/astronomy#influences"), Enceladus);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Enceladus);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Europa);
        owlModel.add(Values.iri("http://example.org/astronomy#Jupiter"), Values.iri("http://example.org/astronomy#hasSatellite"), Europa);
        owlModel.add(Values.iri("http://example.org/astronomy#Jupiter"), Values.iri("http://example.org/astronomy#hasSatellite"), Ganymede);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Ganymede);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Miranda);
        owlModel.add(Values.iri("http://example.org/astronomy#Uranus"), Values.iri("http://example.org/astronomy#hasSatellite"), Miranda);
        owlModel.add(Values.iri("http://example.org/astronomy#Mars"), Values.iri("http://example.org/astronomy#hasSatellite"), Phobos);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Phobos);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Titan);
        owlModel.add(Values.iri("http://example.org/astronomy#Saturn"), Values.iri("http://example.org/astronomy#influences"), Titan);
        owlModel.add(Values.iri("http://example.org/astronomy#Saturn"), Values.iri("http://example.org/astronomy#hasSatellite"), Titan);
        owlModel.add(Values.iri("http://example.org/astronomy#Neptune"), Values.iri("http://example.org/astronomy#hasSatellite"), Triton);
        owlModel.add(Values.iri("http://example.org/astronomy#Sun"), Values.iri("http://example.org/astronomy#surroundedBy"), Triton);

        // 3η ΚΛΑΣΗ: Supernova
        IRI Supernova = Values.iri("http://example.org/astronomy#Supernova");
        IRI SN1993J = Values.iri("http://example.org/astronomy#SN1993J");
        owlModel.add(SN1993J, RDF.TYPE, Supernova);
        owlModel.add(SN1993J, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("10.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(SN1993J, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(259200));
        owlModel.add(SN1993J, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(SN1993J, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(false));
        owlModel.add(SN1993J, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1993-03-28", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(SN1993J, RDFS.LABEL, Values.literal("SN1993J"));

        IRI CrabNebula = Values.iri("http://example.org/astronomy#CrabNebula");
        owlModel.add(CrabNebula, RDF.TYPE, Supernova);
        owlModel.add(CrabNebula, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("-6.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(CrabNebula, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(33120));
        owlModel.add(CrabNebula, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(CrabNebula, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(CrabNebula, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1054-07-04", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(CrabNebula, RDFS.LABEL, Values.literal("Crab Nebula"));

        IRI Tycho = Values.iri("http://example.org/astronomy#Tycho");
        owlModel.add(Tycho, RDF.TYPE, Supernova);
        owlModel.add(Tycho, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("-4.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(Tycho, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(691200));
        owlModel.add(Tycho, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Tycho, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(Tycho, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1572-11-11", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Tycho, RDFS.LABEL, Values.literal("Tycho"));

        IRI Kepler = Values.iri("http://example.org/astronomy#Kepler");
        owlModel.add(Kepler, RDF.TYPE, Supernova);
        owlModel.add(Kepler, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("-2.5", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(Kepler, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(525600));
        owlModel.add(Kepler, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(Kepler, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(Kepler, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1604-10-09", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Kepler, RDFS.LABEL, Values.literal("Kepler"));

        IRI SN1987A = Values.iri("http://example.org/astronomy#SN1987A");
        owlModel.add(SN1987A, RDF.TYPE, Supernova);
        owlModel.add(SN1987A, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("3.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(SN1987A, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(86400));
        owlModel.add(SN1987A, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(SN1987A, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(SN1987A, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1987-02-23", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(SN1987A, RDFS.LABEL, Values.literal("SN1987A"));

        IRI AndromedaSN = Values.iri("http://example.org/astronomy#AndromedaSN");
        owlModel.add(AndromedaSN, RDF.TYPE, Supernova);
        owlModel.add(AndromedaSN, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("-2.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(AndromedaSN, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(172800));
        owlModel.add(AndromedaSN, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(AndromedaSN, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(AndromedaSN, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1885-08-20", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(AndromedaSN, RDFS.LABEL, Values.literal("AndromedaSN"));

        IRI SN1006 = Values.iri("http://example.org/astronomy#SN1006");
        owlModel.add(SN1006, RDF.TYPE, Supernova);
        owlModel.add(SN1006, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("-7.5", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(SN1006, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(100800));
        owlModel.add(SN1006, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(SN1006, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(SN1006, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1006-04-30", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(SN1006, RDFS.LABEL, Values.literal("SN1006"));

        IRI SN1181 = Values.iri("http://example.org/astronomy#SN1181");
        owlModel.add(SN1181, RDF.TYPE, Supernova);
        owlModel.add(SN1181, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("-1.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(SN1181, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(266400));
        owlModel.add(SN1181, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(SN1181, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(SN1181, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1181-08-06", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(SN1181, RDFS.LABEL, Values.literal("SN1181"));

        IRI CasA = Values.iri("http://example.org/astronomy#CasA");
        owlModel.add(CasA, RDF.TYPE, Supernova);
        owlModel.add(CasA, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(525600));
        owlModel.add(CasA, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(CasA, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(true));
        owlModel.add(CasA, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("1574-07-23", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(CasA, RDFS.LABEL, Values.literal("Cas A"));

        IRI SN2006gy = Values.iri("http://example.org/astronomy#SN2006gy");
        owlModel.add(SN2006gy, RDF.TYPE, Supernova);
        owlModel.add(SN2006gy, Values.iri("http://example.org/astronomy#hasBrightness"), Values.literal("14.0", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(SN2006gy, Values.iri("http://example.org/astronomy#hasDuration"), Values.literal(100800));
        owlModel.add(SN2006gy, Values.iri("http://example.org/astronomy#observedFrom"), Values.iri("http://example.org/astronomy#Earth"));
        owlModel.add(SN2006gy, Values.iri("http://example.org/astronomy#visibleToNakedEye"), Values.literal(false));
        owlModel.add(SN2006gy, Values.iri("http://example.org/astronomy#wasObservedOn"), Values.literal("2006-09-18", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(SN2006gy, RDFS.LABEL, Values.literal("SN 2006gy"));

        // 4η ΚΛΑΣΗ: BlueGiantStar
        IRI BlueGiantStar = Values.iri("http://example.org/astronomy#BlueGiantStar");
        IRI SpicaA = Values.iri("http://example.org/astronomy#SpicaA");
        owlModel.add(SpicaA, RDF.TYPE, BlueGiantStar);
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Greeks"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("11000000"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("14000"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("12.3 sextillion metric tons"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("25300"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(SpicaA, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B1_III_IV"));
        owlModel.add(SpicaA, RDFS.LABEL, Values.literal("SpicaA"));

        IRI AlphaVirginis = Values.iri("http://example.org/astronomy#AlphaVirginis");
        owlModel.add(AlphaVirginis, RDF.TYPE, BlueGiantStar);
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Unknown"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("15.4000000"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("310"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("11 million trillion metric tons"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("22400"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B1_III_IV"));
        owlModel.add(AlphaVirginis, Values.iri("http://example.org/astronomy#surroundedBy"), SpicaA);
        owlModel.add(AlphaVirginis, RDFS.LABEL, Values.literal("AlphaVirginis"));

        IRI BetaOrionis = Values.iri("http://example.org/astronomy#BetaOrionis");
        owlModel.add(BetaOrionis, RDF.TYPE, BlueGiantStar);
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Unknown"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("78000000"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("110"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("21.6 million trillion metric tons"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("11000"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B8_Ia"));
        owlModel.add(BetaOrionis, Values.iri("http://example.org/astronomy#surroundedBy"), Values.iri("http://example.org/astronomy#Betelgeuse"));
        owlModel.add(BetaOrionis, RDFS.LABEL, Values.literal("BetaOrionis"));

        IRI AlphaCygni = Values.iri("http://example.org/astronomy#AlphaCygni");
        owlModel.add(AlphaCygni, RDF.TYPE, BlueGiantStar);
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Ancient Astronomers"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("200000000"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("800"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("19 quintillion metric tons"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("8500"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("A2_Ia"));
        owlModel.add(AlphaCygni, Values.iri("http://example.org/astronomy#surroundedBy"), Values.iri("http://example.org/astronomy#Betelgeuse"));
        owlModel.add(AlphaCygni, RDFS.LABEL, Values.literal("AlphaCygni"));

        IRI ZetaOrionis = Values.iri("http://example.org/astronomy#ZetaOrionis");
        owlModel.add(ZetaOrionis, RDF.TYPE, BlueGiantStar);
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Johann Bayer"));
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("32000000"));
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("2500"));
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("58 quintillion metric tons"));
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("29000"));
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(ZetaOrionis, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("O9.5Ib"));
        owlModel.add(ZetaOrionis, RDFS.LABEL, Values.literal("ZetaOrionis"));

        IRI GammaOrionis = Values.iri("http://example.org/astronomy#GammaOrionis");
        owlModel.add(GammaOrionis, RDF.TYPE, BlueGiantStar);
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Ancient Astronomers"));
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("6400000"));
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("3200"));
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("14 quintillion metric tons"));
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("21500"));
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(GammaOrionis, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B2_III"));
        owlModel.add(GammaOrionis, RDFS.LABEL, Values.literal("GammaOrionis"));

        IRI Shaula = Values.iri("http://example.org/astronomy#Shaula");
        owlModel.add(Shaula, RDF.TYPE, BlueGiantStar);
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Ptolemy"));
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("15300000"));
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("4000"));
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("15 quintillion metric tons"));
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("25000"));
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Shaula, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B2_IV"));
        owlModel.add(Shaula, RDFS.LABEL, Values.literal("Shaula"));

        IRI Achernar = Values.iri("http://example.org/astronomy#Achernar");
        owlModel.add(Achernar, RDF.TYPE, BlueGiantStar);
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Al-Sufi"));
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("9300000"));
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("8000"));
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("18 quintillion metric tons"));
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("20000"));
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Achernar, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B6_Vep"));
        owlModel.add(Achernar, RDFS.LABEL, Values.literal("ZetaOrionis"));

        IRI Vega = Values.iri("http://example.org/astronomy#Vega");
        owlModel.add(Vega, RDF.TYPE, BlueGiantStar);
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Ancient Astronomers"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("3000000"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("13900"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("4.8 quintillion metric tons"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("9600"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Vega, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("A0_Va"));
        owlModel.add(Vega, RDFS.LABEL, Values.literal("Vega"));

        IRI Regulus = Values.iri("http://example.org/astronomy#Regulus");
        owlModel.add(Regulus, RDF.TYPE, BlueGiantStar);
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Ancient Astronomers"));
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("4300000"));
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#hasGravity"), Values.literal("15000"));
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("5 quintillion metric tons"));
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("12000"));
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(Regulus, Values.iri("http://example.org/astronomy#spectralType"), Values.literal("B7_V"));
        owlModel.add(Regulus, RDFS.LABEL, Values.literal("Regulus"));

        // 5η ΚΛΑΣΗ: SupermassiveBlackHole
        IRI SupermassiveBlackHole = Values.iri("http://example.org/astronomy#SupermassiveBlackHole");
        IRI SagittariusA = Values.iri("http://example.org/astronomy#SagittariusA");
        owlModel.add(SagittariusA, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Reinhard Genzel and Andrea Ghez"));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1974-01-01", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("44000000"));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("7,956,000,000,000 quadrillion metric tons"));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("100000000"));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(SagittariusA, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#MilkyWay"));
        owlModel.add(SagittariusA, RDFS.LABEL, Values.literal("SagittariusA*"));

        IRI M87 = Values.iri("http://example.org/astronomy#M97");
        owlModel.add(M87, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(M87, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Heino Falcke"));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("2019-04-10", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("40000000000"));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("117 quadrillion quadrillion metric tons"));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#hasSpin"), Values.literal("0.9", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("1000000"));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#EventHorizonTelescope"));
        owlModel.add(M87, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#Messier87"));
        owlModel.add(M87, RDFS.LABEL, Values.literal("M87"));

        IRI M31 = Values.iri("http://example.org/astronomy#M31");
        owlModel.add(M31, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(M31, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Karl Jansky"));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1963-01-01", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("283800000000"));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("40 quadrillion metric tons"));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("100000000"));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#Andromeda"));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(M31, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#Andromeda"));
        owlModel.add(M31, RDFS.LABEL, Values.literal("M31"));

        IRI CentaurusABlackHole = Values.iri("http://example.org/astronomy#CentaurusABlackHole");
        owlModel.add(CentaurusABlackHole, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("John Herschel"));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1847-01-01", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("567.6000000000000"));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("55,000,000,000,000 quadrillion metric tons"));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("100000000"));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#influences"), Values.iri("http://example.org/astronomy#CentaurusA"));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(CentaurusABlackHole, Values.iri("http://example.org/astronomy#partOfGalaxy"), Values.iri("http://example.org/astronomy#CentaurusA"));
        owlModel.add(CentaurusABlackHole, RDFS.LABEL, Values.literal("CentaurusABlackHole"));

        IRI NGC1277 = Values.iri("http://example.org/astronomy#NGC1277");
        owlModel.add(NGC1277, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Remco van den Bosch and team"));
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("2012-11-28", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("1400000000"));
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("20 quintillion quintillion metric tons"));
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("5000000"));
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(NGC1277, Values.iri("http://example.org/astronomy#hasSpin"), Values.literal("0.5", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(NGC1277, RDFS.LABEL, Values.literal("NGC1277"));

        IRI TON618 = Values.iri("http://example.org/astronomy#TON618");
        owlModel.add(TON618, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(TON618, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("John Bahcall and Martin Schmidt"));
        owlModel.add(TON618, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1963-05-15", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(TON618, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("390000000"));
        owlModel.add(TON618, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("66 quintillion quintillion metric tons"));
        owlModel.add(TON618, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("50000"));
        owlModel.add(TON618, RDFS.LABEL, Values.literal("TON618"));

        IRI CygnusA = Values.iri("http://example.org/astronomy#CygnusA");
        owlModel.add(CygnusA, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(CygnusA, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal(" Grote Reber"));
        owlModel.add(CygnusA, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1939-07-19", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(CygnusA, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("25000000000"));
        owlModel.add(CygnusA, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("2.5 quintillion quintillion metric tons"));
        owlModel.add(CygnusA, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("100000"));
        owlModel.add(CygnusA, Values.iri("http://example.org/astronomy#hasSpin"), Values.literal("0.85", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(CygnusA, RDFS.LABEL, Values.literal("CygnusA"));

        IRI NGC4889 = Values.iri("http://example.org/astronomy#NGC4889");
        owlModel.add(NGC4889, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Edwin Hubble"));
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1935-03-12", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("8700000000"));
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("21 quintillion quintillion metric tons"));
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("2000000"));
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#observedByTelescope"), Values.iri("http://example.org/astronomy#HubbleSpaceTelescope"));
        owlModel.add(NGC4889, Values.iri("http://example.org/astronomy#hasSpin"), Values.literal("0.5", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(NGC4889, RDFS.LABEL, Values.literal("NGC4889"));

        IRI PKS0745 = Values.iri("http://example.org/astronomy#PKS0745");
        owlModel.add(PKS0745, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(PKS0745, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Astronomers"));
        owlModel.add(PKS0745, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("1986-09-20", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(PKS0745, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("4600000000"));
        owlModel.add(PKS0745, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("3 quintillion quintillion metric tons"));
        owlModel.add(PKS0745, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("5000000"));
        owlModel.add(PKS0745, RDFS.LABEL, Values.literal("PKS0745"));

        IRI Holm15A = Values.iri("http://example.org/astronomy#Holm15A");
        owlModel.add(Holm15A, RDF.TYPE, SupermassiveBlackHole);
        owlModel.add(Holm15A, Values.iri("http://example.org/astronomy#discoveredBy"), Values.literal("Researchers at the Max Planck Institute for Extraterrestrial Physics"));
        owlModel.add(Holm15A, Values.iri("http://example.org/astronomy#discoveredDate"), Values.literal("2019-06-06", Values.iri("http://www.w3.org/2001/XMLSchema#date")));
        owlModel.add(Holm15A, Values.iri("http://example.org/astronomy#hasDiameter"), Values.literal("1600000000"));
        owlModel.add(Holm15A, Values.iri("http://example.org/astronomy#hasMass"), Values.literal("40 quintillion quintillion metric tons"));
        owlModel.add(Holm15A, Values.iri("http://example.org/astronomy#hasTemperature"), Values.literal("7000000"));
        owlModel.add(Holm15A, Values.iri("http://example.org/astronomy#hasSpin"), Values.literal("0.3", Values.iri("http://www.w3.org/2001/XMLSchema#decimal")));
        owlModel.add(Holm15A, RDFS.LABEL, Values.literal("Holm 15A"));

        // extra declarations
        owlModel.add(Values.iri("http://example.org/astronomy#MilkyWay"), Values.iri("http://example.org/astronomy#hasGalacticCenter"), SagittariusA);
        owlModel.add(Values.iri("http://example.org/astronomy#Messier87"), Values.iri("http://example.org/astronomy#hasGalacticCenter"), M87);
        owlModel.add(Values.iri("http://example.org/astronomy#Andromeda"), Values.iri("http://example.org/astronomy#hasGalacticCenter"), M31);
        owlModel.add(Values.iri("http://example.org/astronomy#CentaurusA"), Values.iri("http://example.org/astronomy#hasGalacticCenter"), CentaurusABlackHole);
    }
}
