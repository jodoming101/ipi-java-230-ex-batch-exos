package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.CommercialRepository;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import com.ipiecoles.java.java230.repository.TechnicienRepository;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import org.joda.time.format.DateTimeFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final String REGEX_MATRICULE_COMMERCIAL = "^C[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private CommercialRepository commercialRepository;

    @Autowired
    private TechnicienRepository technicienRepository;

    private List<Employe> employes = new ArrayList<Employe>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) throws Exception {
        Stream<String> stream;
        stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));

        Integer i = 0;

        for(String ligne : stream.collect(Collectors.toList())) {
            i++;
            try {
                processLine(ligne);
            } catch (BatchException e){
                System.out.println("Ligne " + i + " : " + e.getMessage() + " => " + ligne);
            }
        }
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException, ParseException {
        String[] splitLigne = ligne.split(",");
        if (splitLigne.length >= 5) {
            if (!splitLigne[0].matches("^[MTC]{1}.*")) {
                throw new BatchException("Type d'employe inconnu : " + ligne.charAt(0));
            }
            if (!splitLigne[0].matches(REGEX_MATRICULE)) {
                throw new BatchException("La chaîne " + splitLigne[0] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$");
            }
            String date = splitLigne[3];
            try {
                DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(date);
            } catch (Exception e) {
                throw new BatchException(splitLigne[3] + " ne respecte pas le format de date dd/MM/yyyy");
            }
            String salaire = splitLigne[4];
            try {
                Double.parseDouble(salaire);
            } catch (Exception e) {
                throw new BatchException(salaire + " n'est pas un nombre valide pour un salaire");
            }
            processManager(ligne);
            processCommercial(ligne);
            processTechnicien(ligne);
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        String[] splitLigneCommercial = ligneCommercial.split(",");
        if (splitLigneCommercial[0].matches(REGEX_MATRICULE_COMMERCIAL)) {
            if (splitLigneCommercial.length != NB_CHAMPS_COMMERCIAL) {
                throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + splitLigneCommercial.length);
            }
            String CaCommercial = splitLigneCommercial[5];
            try {
                Double.parseDouble(CaCommercial);
            } catch (Exception e) {
                throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + CaCommercial);
            }
            String PerfCommercial = splitLigneCommercial[6];
            try {
                Integer.parseInt(PerfCommercial);
            } catch (Exception e) {
                throw new BatchException("La performance du commercial est incorrecte : " + PerfCommercial);
            }
            String nomCommercial = splitLigneCommercial[1];
            String prenomCommercial = splitLigneCommercial[2];
            String matriculeCommercial = splitLigneCommercial[0];
            LocalDate dateEmbauche = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(splitLigneCommercial[3]);
            Double salaireCommercial = Double.parseDouble(splitLigneCommercial[4]);
            Double caCom = Double.parseDouble(CaCommercial);
            Integer perfCom = Integer.parseInt(PerfCommercial);
            Commercial c = new Commercial(nomCommercial, prenomCommercial, matriculeCommercial, dateEmbauche, salaireCommercial, caCom, perfCom);
            employes.add(c);
        }
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        String[] splitLigneManager = ligneManager.split(",");
        if (splitLigneManager[0].matches(REGEX_MATRICULE_MANAGER)) {
            if (splitLigneManager.length != NB_CHAMPS_MANAGER) {
                throw new BatchException("La ligne manager ne contient pas 5 éléments mais " + splitLigneManager.length);
            }
            String nomManager = splitLigneManager[1];
            String prenomManager = splitLigneManager[2];
            String matriculeManager = splitLigneManager[0];
            LocalDate dateEmbauche = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(splitLigneManager[3]);
            Double salaireManager = Double.parseDouble(splitLigneManager[4]);
            Manager m = new Manager(nomManager, prenomManager, matriculeManager, dateEmbauche, salaireManager);
            employes.add(m);
        }
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        String[] splitLigneTechnicien = ligneTechnicien.split(",");
        if (splitLigneTechnicien[0].matches("^T{1}.*")) {
            if (splitLigneTechnicien.length != NB_CHAMPS_TECHNICIEN) {
                throw new BatchException("La ligne technicien ne contient pas 7 éléments mais " + splitLigneTechnicien.length);
            }
            if (!splitLigneTechnicien[6].matches(REGEX_MATRICULE_MANAGER)) {
                throw new BatchException("La chaine " + splitLigneTechnicien[6] + " ne respecte pas l'expression régulière ^M[0-9]{5}$");
            }
            try {
                Integer.parseInt(splitLigneTechnicien[5]);
            } catch (Exception e) {
                throw new BatchException("Le grade du technicien est incorrect : " + splitLigneTechnicien[5]);
            }
            if (Integer.parseInt(splitLigneTechnicien[5]) < 1 || Integer.parseInt(splitLigneTechnicien[5]) > 5) {
                throw new BatchException("Le grade doit être compris entre 1 et 5 : " + splitLigneTechnicien[5] + ", technicien : Technicien{grade=null} Employe{nom='null', prenom='null', matricule='null', dateEmbauche=null, salaire=1480.27}");
            }
            String managerTechnicien = splitLigneTechnicien[6];
            if (managerRepository.findByMatricule(managerTechnicien) == null) {
                throw new BatchException("Le manager de matricule " + managerTechnicien + " n'a pas été trouvé dans le fichier ou en base de données");
            }
            String nomTechnicien = splitLigneTechnicien[1];
            String prenomTechnicien = splitLigneTechnicien[2];
            String matriculeTechnicien = splitLigneTechnicien[0];
            LocalDate dateEmbauche = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(splitLigneTechnicien[3]);
            Double salaireTechnicien = Double.parseDouble(splitLigneTechnicien[4]);
            Integer gradeTechnicien = Integer.parseInt(splitLigneTechnicien[5]);
            Technicien t = new Technicien(nomTechnicien, prenomTechnicien, matriculeTechnicien, dateEmbauche, salaireTechnicien, gradeTechnicien);
            employes.add(t);
        }
    }
}
