package com.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.util.Arrays;
import java.util.Scanner;

import static com.mongodb.client.model.Filters.*;

public class App {

    private static MongoCollection<Document> students;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        String uri = System.getenv("MONGODB_URI");

        if (uri == null || uri.isBlank()) {
            System.out.println("Please set MONGODB_URI first.");
            return;
        }

        try (MongoClient client = MongoClients.create(uri)) {

            MongoDatabase database = client.getDatabase("college");
            students = database.getCollection("students");

            database.runCommand(new Document("ping", 1));

            System.out.println("MongoDB connected successfully!");
            System.out.println("Database: " + database.getName());
            System.out.println("Collection: " + students.getNamespace().getCollectionName());

            mainMenu();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void mainMenu() {

        while (true) {
            System.out.println("\n===== STUDENT MANAGEMENT SYSTEM =====");
            System.out.println("1. Add Student");
            System.out.println("2. View Students");
            System.out.println("3. Search Students");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    addStudent();
                    break;
                case "2":
                    viewStudents();
                    break;
                case "3":
                    searchMenu();
                    break;
                case "4":
                    System.out.println("Exiting application. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    // TASK 2: INSERT STUDENT

    private static void addStudent() {

        try {
            System.out.println("\n===== ADD STUDENT =====");

            System.out.print("Enter Student ID: ");
            String id = sc.nextLine().trim();

            if (id.isEmpty()) {
                System.out.println("Student ID cannot be empty.");
                return;
            }

            if (students.find(eq("studentId", id)).first() != null) {
                System.out.println("Student ID already exists!");
                return;
            }

            System.out.print("Enter Student Name: ");
            String name = sc.nextLine().trim();

            System.out.print("Enter Department: ");
            String department = sc.nextLine().trim();

            System.out.print("Enter CGPA: ");
            double cgpa = Double.parseDouble(sc.nextLine().trim());

            if (cgpa < 0 || cgpa > 10) {
                System.out.println("CGPA must be between 0 and 10.");
                return;
            }

            System.out.print("Enter Skills separated by commas: ");
            String skillInput = sc.nextLine();

            String[] skillArray = Arrays.stream(skillInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);

            Document student = new Document("studentId", id)
                    .append("name", name)
                    .append("department", department)
                    .append("cgpa", cgpa)
                    .append("skills", Arrays.asList(skillArray));

            students.insertOne(student);

            System.out.println("Student added successfully!");

        } catch (NumberFormatException e) {
            System.out.println("Invalid CGPA. Please enter a number.");
        } catch (Exception e) {
            System.out.println("Unable to add student: " + e.getMessage());
        }
    }

    // TASK 2: VIEW ALL STUDENTS

    private static void viewStudents() {

        System.out.println("\n===== ALL STUDENTS =====");

        try {
            boolean found = false;

            for (Document student : students.find()) {
                System.out.println(student.toJson());
                found = true;
            }

            if (!found) {
                System.out.println("No students found.");
            }

        } catch (Exception e) {
            System.out.println("Unable to view students: " + e.getMessage());
        }
    }

    // TASK 3: QUERY MENU

    private static void searchMenu() {

        while (true) {
            System.out.println("\n===== SEARCH STUDENTS =====");
            System.out.println("1. Search by Student ID");
            System.out.println("2. Search by Department");
            System.out.println("3. Search by Minimum CGPA");
            System.out.println("4. Search by Skill");
            System.out.println("5. View All Students");
            System.out.println("6. Back to Main Menu");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    searchById();
                    break;
                case "2":
                    searchByDepartment();
                    break;
                case "3":
                    searchByCgpa();
                    break;
                case "4":
                    searchBySkill();
                    break;
                case "5":
                    viewStudents();
                    break;
                case "6":
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    // SEARCH BY ID

    private static void searchById() {

        System.out.print("Enter Student ID: ");
        String id = sc.nextLine().trim();

        displayResults(students.find(eq("studentId", id)));
    }

    // SEARCH BY DEPARTMENT

    private static void searchByDepartment() {

        System.out.print("Enter Department: ");
        String department = sc.nextLine().trim();

        displayResults(
                students.find(regex("department", "^" +
                        java.util.regex.Pattern.quote(department) + "$", "i"))
        );
    }

    // SEARCH BY MINIMUM CGPA

    private static void searchByCgpa() {

        try {
            System.out.print("Enter minimum CGPA: ");
            double cgpa = Double.parseDouble(sc.nextLine().trim());

            if (cgpa < 0 || cgpa > 10) {
                System.out.println("CGPA must be between 0 and 10.");
                return;
            }

            displayResults(students.find(gte("cgpa", cgpa)));

        } catch (NumberFormatException e) {
            System.out.println("Invalid CGPA. Please enter a number.");
        }
    }

    // SEARCH BY SKILL

    private static void searchBySkill() {

        System.out.print("Enter Skill: ");
        String skill = sc.nextLine().trim();

        displayResults(
                students.find(regex("skills",
                        "^" + java.util.regex.Pattern.quote(skill) + "$", "i"))
        );
    }

    // DISPLAY QUERY RESULTS

    private static void displayResults(FindIterable<Document> results) {

        boolean found = false;

        for (Document student : results) {
            System.out.println(student.toJson());
            found = true;
        }

        if (!found) {
            System.out.println("No matching students found.");
        }
    }
}