
package com.example;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public class App {
    static Scanner sc = new Scanner(System.in);
    static MongoClient client;
    static MongoDatabase db;

    static MongoCollection<Document> students;
    static MongoCollection<Document> companies;
    static MongoCollection<Document> jobDrives;
    static MongoCollection<Document> applications;

    public static void main(String[] args) {
        String uri = System.getenv("MONGODB_URI");

        if (uri == null || uri.isBlank()) {
            System.out.println("Please set the MONGODB_URI environment variable.");
            return;
        }

        try {
            client = MongoClients.create(uri);
            db = client.getDatabase("college");

            students = db.getCollection("students");
            companies = db.getCollection("companies");
            jobDrives = db.getCollection("jobDrives");
            applications = db.getCollection("applications");

            System.out.println("Connected to MongoDB successfully!");
            menu();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            if (client != null) client.close();
        }
    }

    static void menu() {
        while (true) {
            System.out.println("\n===== STUDENT PLACEMENT SYSTEM =====");
            System.out.println("1. Student Module");
            System.out.println("2. Company Module");
            System.out.println("3. Job Drive Module");
            System.out.println("4. Application Module");
            System.out.println("5. Candidate Search");
            System.out.println("6. Placement Analytics");
            System.out.println("7. View All Records");
            System.out.println("0. Exit");

            int choice = integer("Enter choice: ");

            switch (choice) {
                case 1 -> studentMenu();
                case 2 -> companyMenu();
                case 3 -> driveMenu();
                case 4 -> applicationMenu();
                case 5 -> searchCandidates();
                case 6 -> analytics();
                case 7 -> viewAll();
                case 0 -> {
                    System.out.println("Program closed.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ================ STUDENT MODULE ================

    static void studentMenu() {
        System.out.println("\n1. Add Student");
        System.out.println("2. View Students");
        System.out.println("3. Update CGPA");
        System.out.println("4. Delete Student");

        int choice = integer("Choice: ");

        switch (choice) {
            case 1 -> addStudent();
            case 2 -> show(students.find());
            case 3 -> updateCGPA();
            case 4 -> {
                String id = input("Student ID: ");
                System.out.println(students.deleteOne(
                    Filters.eq("studentId", id)
                ).getDeletedCount() + " student(s) deleted.");
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    static void addStudent() {
        String id = input("Student ID: ");

        if (students.find(Filters.eq("studentId", id)).first() != null) {
            System.out.println("Student ID already exists.");
            return;
        }

        String name = input("Name: ");
        String email = input("Email: ");
        String department = input("Department: ");
        double cgpa = decimal("CGPA (0-10): ");

        if (cgpa < 0 || cgpa > 10) {
            System.out.println("CGPA must be between 0 and 10.");
            return;
        }

        List<String> skills = Arrays.stream(
                input("Skills separated by commas: ").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        Document student = new Document("studentId", id)
                .append("name", name)
                .append("email", email)
                .append("department", department)
                .append("cgpa", cgpa)
                .append("skills", skills)
                .append("placed", false);

        students.insertOne(student);
        System.out.println("Student added successfully.");
    }

    static void updateCGPA() {
        String id = input("Student ID: ");
        double cgpa = decimal("New CGPA: ");

        if (cgpa < 0 || cgpa > 10) {
            System.out.println("Invalid CGPA.");
            return;
        }

        long count = students.updateOne(
                Filters.eq("studentId", id),
                new Document("$set", new Document("cgpa", cgpa))
        ).getMatchedCount();

        System.out.println(count > 0 ? "CGPA updated." : "Student not found.");
    }

    // ================ COMPANY MODULE ================

    static void companyMenu() {
        System.out.println("\n1. Add Company");
        System.out.println("2. View Companies");
        System.out.println("3. Delete Company");

        int choice = integer("Choice: ");

        switch (choice) {
            case 1 -> {
                Document company = new Document(
                        "name", input("Company name: "))
                        .append("industry", input("Industry: "))
                        .append("location", input("Location: "))
                        .append("website", input("Website: "));

                companies.insertOne(company);
                System.out.println("Company added.");
                System.out.println("Company MongoDB ID: " +
                        company.getObjectId("_id"));
            }
            case 2 -> show(companies.find());
            case 3 -> deleteByMongoId(companies, "Company");
            default -> System.out.println("Invalid choice.");
        }
    }

    // ================ JOB DRIVE MODULE ================

    static void driveMenu() {
        System.out.println("\n1. Create Job Drive");
        System.out.println("2. View Job Drives");
        System.out.println("3. Delete Job Drive");

        int choice = integer("Choice: ");

        switch (choice) {
            case 1 -> createDrive();
            case 2 -> show(jobDrives.find());
            case 3 -> deleteByMongoId(jobDrives, "Job drive");
            default -> System.out.println("Invalid choice.");
        }
    }

    static void createDrive() {
        show(companies.find());

        String companyId = input("Company MongoDB ID: ");
        Document company = findById(companies, companyId);

        if (company == null) {
            System.out.println("Company not found. Add it first.");
            return;
        }

        double minCgpa = decimal("Minimum CGPA: ");
        if (minCgpa < 0 || minCgpa > 10) {
            System.out.println("Invalid CGPA.");
            return;
        }

        Document drive = new Document("companyId", companyId)
                .append("companyName", company.getString("name"))
                .append("role", input("Job role: "))
                .append("department", input("Eligible department (or ALL): "))
                .append("minCgpa", minCgpa)
                .append("packageLpa", decimal("Package in LPA: "))
                .append("status", "OPEN");

        jobDrives.insertOne(drive);

        System.out.println("Job drive created.");
        System.out.println("Job Drive MongoDB ID: " + drive.getObjectId("_id"));
    }

    // ================ APPLICATION MODULE ================

    static void applicationMenu() {
        System.out.println("\n1. Apply for Job");
        System.out.println("2. View Applications");
        System.out.println("3. Update Application Status");

        int choice = integer("Choice: ");

        switch (choice) {
            case 1 -> applyForJob();
            case 2 -> show(applications.find());
            case 3 -> updateApplication();
            default -> System.out.println("Invalid choice.");
        }
    }

    static void applyForJob() {
        show(students.find());

        String studentId = input("Student ID: ");
        Document student = students.find(
                Filters.eq("studentId", studentId)).first();

        if (student == null) {
            System.out.println("Student not found.");
            return;
        }

        show(jobDrives.find(Filters.eq("status", "OPEN")));
        String driveId = input("Job Drive MongoDB ID: ");
        Document drive = findById(jobDrives, driveId);

        if (drive == null ||
                !"OPEN".equals(drive.getString("status"))) {
            System.out.println("Open job drive not found.");
            return;
        }

        if (applications.find(Filters.and(
                Filters.eq("studentId", studentId),
                Filters.eq("jobDriveId", driveId)
        )).first() != null) {
            System.out.println("Already applied for this drive.");
            return;
        }

        double cgpa = student.get("cgpa", Number.class).doubleValue();
        double minimum = drive.get("minCgpa", Number.class).doubleValue();
        String eligibleDepartment = drive.getString("department");
        String studentDepartment = student.getString("department");

        if (cgpa < minimum ||
                (!"ALL".equalsIgnoreCase(eligibleDepartment) &&
                !eligibleDepartment.equalsIgnoreCase(studentDepartment))) {
            System.out.println("Student is not eligible.");
            return;
        }

        Document application = new Document("studentId", studentId)
                .append("studentName", student.getString("name"))
                .append("jobDriveId", driveId)
                .append("companyName", drive.getString("companyName"))
                .append("role", drive.getString("role"))
                .append("status", "APPLIED");

        applications.insertOne(application);
        System.out.println("Application submitted successfully.");
        System.out.println("Application ID: " + application.getObjectId("_id"));
    }

    static void updateApplication() {
        show(applications.find());

        String id = input("Application MongoDB ID: ");
        if (!ObjectId.isValid(id)) {
            System.out.println("Invalid ID.");
            return;
        }

        Document application = applications.find(
                Filters.eq("_id", new ObjectId(id))).first();

        if (application == null) {
            System.out.println("Application not found.");
            return;
        }

        String status = input(
                "Status (APPLIED/SHORTLISTED/SELECTED/REJECTED): ")
                .toUpperCase();

        if (!List.of("APPLIED", "SHORTLISTED", "SELECTED", "REJECTED")
                .contains(status)) {
            System.out.println("Invalid status.");
            return;
        }

        applications.updateOne(
                Filters.eq("_id", new ObjectId(id)),
                new Document("$set", new Document("status", status))
        );

        if ("SELECTED".equals(status)) {
            students.updateOne(
                    Filters.eq("studentId", application.getString("studentId")),
                    new Document("$set", new Document("placed", true))
            );
        } else {
            String studentId = application.getString("studentId");

            boolean anotherSelection = applications.find(Filters.and(
                    Filters.eq("studentId", studentId),
                    Filters.eq("status", "SELECTED"),
                    Filters.ne("_id", new ObjectId(id))
            )).first() != null;

            if (!anotherSelection) {
                students.updateOne(
                        Filters.eq("studentId", studentId),
                        new Document("$set", new Document("placed", false))
                );
            }
        }

        System.out.println("Application status updated.");
    }

    // ================ CANDIDATE SEARCH ================

    static void searchCandidates() {
        double minCgpa = decimal("Minimum CGPA: ");
        String department = input("Department (blank for any): ");
        String skill = input("Skill (blank for any): ");

        for (Document s : students.find(
                Filters.gte("cgpa", minCgpa))) {

            String dept = s.getString("department");

            if (!department.isBlank() &&
                    !department.equalsIgnoreCase(dept)) {
                continue;
            }

            List<String> skills = s.getList(
                    "skills", String.class, new ArrayList<>());

            if (!skill.isBlank() &&
                    skills.stream().noneMatch(
                            x -> x.equalsIgnoreCase(skill))) {
                continue;
            }

            System.out.println(s.toJson());
        }
    }

    // ================ PLACEMENT ANALYTICS ================

    static void analytics() {
        long totalStudents = students.countDocuments();
        long placed = students.countDocuments(
                Filters.eq("placed", true));
        long totalApplications = applications.countDocuments();
        long selected = applications.countDocuments(
                Filters.eq("status", "SELECTED"));
        long totalCompanies = companies.countDocuments();
        long totalDrives = jobDrives.countDocuments();

        double percentage = totalStudents == 0 ? 0 :
                placed * 100.0 / totalStudents;

        System.out.println("\n===== PLACEMENT ANALYTICS =====");
        System.out.println("Total students: " + totalStudents);
        System.out.println("Placed students: " + placed);
        System.out.printf("Placement percentage: %.2f%%%n", percentage);
        System.out.println("Total companies: " + totalCompanies);
        System.out.println("Total job drives: " + totalDrives);
        System.out.println("Total applications: " + totalApplications);
        System.out.println("Selected applications: " + selected);

        System.out.println("\nSelected candidates by company:");

        for (Document result : applications.aggregate(List.of(
                new Document("$match",
                        new Document("status", "SELECTED")),
                new Document("$group",
                        new Document("_id", "$companyName")
                                .append("selectedCount",
                                        new Document("$sum", 1))),
                new Document("$sort",
                        new Document("selectedCount", -1))
        ))) {
            System.out.println(result.toJson());
        }

        System.out.println("\nAverage package (LPA):");

        Document average = jobDrives.aggregate(List.of(
                new Document("$group",
                        new Document("_id", null)
                                .append("averagePackage",
                                        new Document("$avg", "$packageLpa")))
        )).first();

        if (average == null || average.get("averagePackage") == null) {
            System.out.println("No package data available.");
        } else {
            System.out.println(average.get("averagePackage"));
        }
    }

    // ================ COMMON METHODS ================

    static void viewAll() {
        System.out.println("\n--- STUDENTS ---");
        show(students.find());

        System.out.println("\n--- COMPANIES ---");
        show(companies.find());

        System.out.println("\n--- JOB DRIVES ---");
        show(jobDrives.find());

        System.out.println("\n--- APPLICATIONS ---");
        show(applications.find());
    }

    static Document findById(
            MongoCollection<Document> collection, String id) {
        if (!ObjectId.isValid(id)) return null;

        return collection.find(
                Filters.eq("_id", new ObjectId(id))).first();
    }

    static void deleteByMongoId(
            MongoCollection<Document> collection, String label) {
        show(collection.find());
        String id = input(label + " MongoDB ID: ");

        if (!ObjectId.isValid(id)) {
            System.out.println("Invalid ID.");
            return;
        }

        long count = collection.deleteOne(
                Filters.eq("_id", new ObjectId(id))).getDeletedCount();

        System.out.println(count > 0 ? label + " deleted." : "Record not found.");
    }

    static void show(FindIterable<Document> records) {
        boolean found = false;

        for (Document doc : records) {
            found = true;
            System.out.println(doc.toJson());
        }

        if (!found) System.out.println("No records found.");
    }

    static String input(String message) {
        System.out.print(message);
        return sc.nextLine().trim();
    }

    static int integer(String message) {
        while (true) {
            try {
                return Integer.parseInt(input(message));
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    static double decimal(String message) {
        while (true) {
            try {
                return Double.parseDouble(input(message));
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }
}
