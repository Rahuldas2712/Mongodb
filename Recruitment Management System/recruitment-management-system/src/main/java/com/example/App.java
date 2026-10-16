package com.example;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.*;

public class App {
    static final Scanner sc = new Scanner(System.in);
    static MongoClient client;
    static MongoDatabase db;
    static MongoCollection<Document> students, companies, jobs, applications, interviews;

    public static void main(String[] args) {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            System.out.println("Set MONGODB_URI before running.");
            return;
        }
        try {
            client = MongoClients.create(uri);
            db = client.getDatabase("college");
            students = db.getCollection("students");
            companies = db.getCollection("companies");
            jobs = db.getCollection("jobDrives");
            applications = db.getCollection("applications");
            interviews = db.getCollection("interviews");
            db.runCommand(new Document("ping", 1));
            System.out.println("Connected to MongoDB: college");
            menu();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            if (client != null) client.close();
        }
    }

    static void menu() {
        while (true) {
            System.out.println("\n===== RECRUITMENT MANAGEMENT SYSTEM =====");
            System.out.println("1 Student Registration");
            System.out.println("2 Company Management");
            System.out.println("3 Job Posting");
            System.out.println("4 Application Tracking");
            System.out.println("5 Interview Scheduling");
            System.out.println("6 Placement Analytics");
            System.out.println("7 View All Records");
            System.out.println("0 Exit");
            switch (intInput("Choice: ")) {
                case 1 -> studentMenu();
                case 2 -> companyMenu();
                case 3 -> jobMenu();
                case 4 -> applicationMenu();
                case 5 -> interviewMenu();
                case 6 -> analytics();
                case 7 -> viewAll();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    static void studentMenu() {
        System.out.println("1 Register  2 View  3 Update CGPA  4 Update Email  5 Add Skills  6 Delete");
        switch (intInput("Choice: ")) {
            case 1 -> {
                String id = required("Student ID: ");
                if (students.find(Filters.eq("studentId", id)).first() != null) {
                    System.out.println("ID already exists."); return;
                }
                double cgpa = doubleInput("CGPA (0-10): ");
                if (cgpa < 0 || cgpa > 10) { System.out.println("Invalid CGPA."); return; }
                students.insertOne(new Document("studentId", id)
                    .append("name", required("Name: "))
                    .append("email", required("Email: "))
                    .append("department", required("Department: "))
                    .append("cgpa", cgpa).append("skills", csv("Skills (comma-separated): "))
                    .append("placed", false));
                System.out.println("Student registered.");
            }
            case 2 -> show(students.find());
            case 3 -> updateStudent("cgpa");
            case 4 -> updateStudent("email");
            case 5 -> {
                String id = required("Student ID: ");
                List<String> skills = csv("Skills to add, comma-separated: ");
                var r = students.updateOne(Filters.eq("studentId", id),
                    new Document("$addToSet", new Document("skills", new Document("$each", skills))));
                System.out.println(r.getMatchedCount() > 0 ? "Skills updated." : "Student not found.");
            }
            case 6 -> {
                String id = required("Student ID: ");
                System.out.println(students.deleteOne(Filters.eq("studentId", id)).getDeletedCount() + " deleted.");
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    static void updateStudent(String field) {
        String id = required("Student ID: ");
        Object value;
        if (field.equals("cgpa")) {
            double n = doubleInput("New CGPA: ");
            if (n < 0 || n > 10) { System.out.println("Invalid CGPA."); return; }
            value = n;
        } else value = required("New email: ");
        long n = students.updateOne(Filters.eq("studentId", id),
            new Document("$set", new Document(field, value))).getMatchedCount();
        System.out.println(n > 0 ? "Updated." : "Student not found.");
    }

    static void companyMenu() {
        System.out.println("1 Add Company  2 View Companies  3 Delete Company");
        switch (intInput("Choice: ")) {
            case 1 -> {
                Document d = new Document("name", required("Company name: "))
                    .append("industry", required("Industry: "))
                    .append("location", required("Location: "))
                    .append("website", line("Website (optional): "));
                companies.insertOne(d);
                System.out.println("Company added. MongoDB ID: " + d.getObjectId("_id"));
            }
            case 2 -> show(companies.find());
            case 3 -> deleteById(companies, "company");
            default -> System.out.println("Invalid choice.");
        }
    }

    static void jobMenu() {
        System.out.println("1 Create Job  2 View Jobs  3 Close Job  4 Delete Job");
        switch (intInput("Choice: ")) {
            case 1 -> {
                show(companies.find());
                String cid = required("Company MongoDB _id: ");
                Document c = byId(companies, cid);
                if (c == null) { System.out.println("Company not found."); return; }
                double min = doubleInput("Minimum CGPA (0-10): ");
                if (min < 0 || min > 10) { System.out.println("Invalid CGPA."); return; }
                Document j = new Document("companyId", cid)
                    .append("companyName", c.getString("name"))
                    .append("role", required("Job role: "))
                    .append("department", required("Eligible department or ALL: "))
                    .append("minCgpa", min)
                    .append("packageLpa", doubleInput("Package in LPA: "))
                    .append("requiredSkills", csv("Required skills, comma-separated: "))
                    .append("status", "OPEN");
                jobs.insertOne(j);
                System.out.println("Job created. MongoDB ID: " + j.getObjectId("_id"));
            }
            case 2 -> show(jobs.find());
            case 3 -> {
                show(jobs.find()); String id = required("Job MongoDB _id: ");
                if (!ObjectId.isValid(id)) { System.out.println("Invalid ID."); return; }
                long n = jobs.updateOne(Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", new Document("status", "CLOSED"))).getMatchedCount();
                System.out.println(n > 0 ? "Job closed." : "Job not found.");
            }
            case 4 -> deleteById(jobs, "job");
            default -> System.out.println("Invalid choice.");
        }
    }

    static void applicationMenu() {
        System.out.println("1 Apply for Job  2 View Applications  3 Update Status");
        switch (intInput("Choice: ")) {
            case 1 -> {
                show(students.find()); String sid = required("Student ID: ");
                Document s = students.find(Filters.eq("studentId", sid)).first();
                if (s == null) { System.out.println("Student not found."); return; }
                show(jobs.find(Filters.eq("status", "OPEN")));
                String jid = required("Job MongoDB _id: "); Document j = byId(jobs, jid);
                if (j == null || !"OPEN".equals(j.getString("status"))) { System.out.println("Open job not found."); return; }
                if (applications.find(Filters.and(Filters.eq("studentId", sid), Filters.eq("jobDriveId", jid))).first() != null) {
                    System.out.println("Already applied."); return;
                }
                double cgpa = number(s.get("cgpa"));
                String dept = j.getString("department");
                if (cgpa < number(j.get("minCgpa")) ||
                    (!"ALL".equalsIgnoreCase(dept) && !dept.equalsIgnoreCase(s.getString("department")))) {
                    System.out.println("Student is not eligible."); return;
                }
                List<String> req = strings(j.get("requiredSkills")), have = strings(s.get("skills"));
                if (!req.isEmpty() && req.stream().noneMatch(r -> have.stream().anyMatch(h -> h.equalsIgnoreCase(r)))) {
                    System.out.println("No matching required skills."); return;
                }
                Document a = new Document("studentId", sid).append("studentName", s.getString("name"))
                    .append("jobDriveId", jid).append("companyName", j.getString("companyName"))
                    .append("role", j.getString("role")).append("status", "APPLIED");
                applications.insertOne(a);
                System.out.println("Application created. MongoDB ID: " + a.getObjectId("_id"));
            }
            case 2 -> show(applications.find());
            case 3 -> {
                show(applications.find()); String id = required("Application MongoDB _id: ");
                if (!ObjectId.isValid(id)) { System.out.println("Invalid ID."); return; }
                Document a = byId(applications, id);
                if (a == null) { System.out.println("Application not found."); return; }
                String status = required("Status APPLIED/SHORTLISTED/SELECTED/REJECTED: ").toUpperCase();
                if (!List.of("APPLIED","SHORTLISTED","SELECTED","REJECTED").contains(status)) {
                    System.out.println("Invalid status."); return;
                }
                applications.updateOne(Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", new Document("status", status)));
                String sid = a.getString("studentId");
                if (status.equals("SELECTED")) {
                    students.updateOne(Filters.eq("studentId", sid), new Document("$set", new Document("placed", true)));
                } else {
                    boolean selected = applications.find(Filters.and(Filters.eq("studentId", sid),
                        Filters.eq("status", "SELECTED"), Filters.ne("_id", new ObjectId(id)))).first() != null;
                    if (!selected) students.updateOne(Filters.eq("studentId", sid),
                        new Document("$set", new Document("placed", false)));
                }
                System.out.println("Application status updated.");
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    static void interviewMenu() {
        System.out.println("1 Schedule Interview  2 View Interviews  3 Reschedule  4 Update Result");
        switch (intInput("Choice: ")) {
            case 1 -> {
                show(applications.find()); String aid = required("Application MongoDB _id: ");
                Document a = byId(applications, aid);
                if (a == null) { System.out.println("Application not found."); return; }
                Document i = new Document("applicationId", aid).append("studentId", a.getString("studentId"))
                    .append("studentName", a.getString("studentName")).append("companyName", a.getString("companyName"))
                    .append("role", a.getString("role")).append("round", required("Interview round: "))
                    .append("interviewDate", required("Date YYYY-MM-DD: "))
                    .append("interviewTime", required("Time HH:MM: "))
                    .append("mode", required("Mode Online/Offline: "))
                    .append("locationOrLink", line("Venue or meeting link: "))
                    .append("status", "SCHEDULED").append("result", "PENDING");
                interviews.insertOne(i);
                System.out.println("Interview scheduled. MongoDB ID: " + i.getObjectId("_id"));
            }
            case 2 -> show(interviews.find());
            case 3 -> {
                show(interviews.find()); String id = required("Interview MongoDB _id: ");
                if (!ObjectId.isValid(id)) { System.out.println("Invalid ID."); return; }
                long n = interviews.updateOne(Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", new Document("interviewDate", required("New date YYYY-MM-DD: "))
                        .append("interviewTime", required("New time HH:MM: ")).append("status", "RESCHEDULED"))).getMatchedCount();
                System.out.println(n > 0 ? "Interview rescheduled." : "Interview not found.");
            }
            case 4 -> {
                show(interviews.find()); String id = required("Interview MongoDB _id: ");
                if (!ObjectId.isValid(id)) { System.out.println("Invalid ID."); return; }
                String status = required("Status SCHEDULED/COMPLETED/CANCELLED: ").toUpperCase();
                String result = required("Result PENDING/PASSED/FAILED: ").toUpperCase();
                if (!List.of("SCHEDULED","COMPLETED","CANCELLED","RESCHEDULED").contains(status) ||
                    !List.of("PENDING","PASSED","FAILED").contains(result)) {
                    System.out.println("Invalid status or result."); return;
                }
                long n = interviews.updateOne(Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", new Document("status", status).append("result", result))).getMatchedCount();
                System.out.println(n > 0 ? "Interview updated." : "Interview not found.");
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    static void analytics() {
        long total = students.countDocuments(), placed = students.countDocuments(Filters.eq("placed", true));
        System.out.println("\n===== PLACEMENT ANALYTICS =====");
        System.out.println("Students: " + total + " | Placed: " + placed);
        System.out.printf("Placement percentage: %.2f%%%n", total == 0 ? 0 : placed * 100.0 / total);
        System.out.println("Companies: " + companies.countDocuments());
        System.out.println("Job postings: " + jobs.countDocuments() + " | Open: " + jobs.countDocuments(Filters.eq("status","OPEN")));
        System.out.println("Applications: " + applications.countDocuments());
        System.out.println("Selected applications: " + applications.countDocuments(Filters.eq("status","SELECTED")));
        System.out.println("Interviews: " + interviews.countDocuments());
        System.out.println("Completed interviews: " + interviews.countDocuments(Filters.eq("status","COMPLETED")));
        System.out.println("Selected candidates by company:");
        for (Document d : applications.aggregate(List.of(
            new Document("$match", new Document("status","SELECTED")),
            new Document("$group", new Document("_id","$companyName").append("count",new Document("$sum",1))),
            new Document("$sort",new Document("count",-1))))) System.out.println(d.toJson());
        Document avg = jobs.aggregate(List.of(new Document("$group",
            new Document("_id",null).append("averagePackageLpa",new Document("$avg","$packageLpa"))))).first();
        System.out.println("Average advertised package (LPA): " +
            (avg == null ? "No data" : avg.get("averagePackageLpa")));
    }

    static void viewAll() {
        System.out.println("\nSTUDENTS"); show(students.find());
        System.out.println("\nCOMPANIES"); show(companies.find());
        System.out.println("\nJOB POSTINGS"); show(jobs.find());
        System.out.println("\nAPPLICATIONS"); show(applications.find());
        System.out.println("\nINTERVIEWS"); show(interviews.find());
    }

    static Document byId(MongoCollection<Document> c, String id) {
        if (!ObjectId.isValid(id)) return null;
        return c.find(Filters.eq("_id", new ObjectId(id))).first();
    }
    static void deleteById(MongoCollection<Document> c, String label) {
        show(c.find()); String id = required(label + " MongoDB _id: ");
        if (!ObjectId.isValid(id)) { System.out.println("Invalid ID."); return; }
        System.out.println(c.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0 ? "Deleted." : "Not found.");
    }
    static void show(FindIterable<Document> records) {
        boolean found = false;
        for (Document d : records) { found = true; System.out.println(d.toJson()); }
        if (!found) System.out.println("No records found.");
    }
    static List<String> csv(String prompt) {
        String v = line(prompt); List<String> out = new ArrayList<>();
        if (!v.isBlank()) for (String s : v.split(",")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }
    static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) if (x != null) out.add(x.toString());
        return out;
    }
    static double number(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
    static String line(String p) { System.out.print(p); return sc.nextLine().trim(); }
    static String required(String p) {
        while (true) { String s = line(p); if (!s.isBlank()) return s; System.out.println("Cannot be empty."); }
    }
    static int intInput(String p) {
        while (true) { try { return Integer.parseInt(line(p)); } catch (NumberFormatException e) { System.out.println("Enter a whole number."); } }
    }
    static double doubleInput(String p) {
        while (true) { try { return Double.parseDouble(line(p)); } catch (NumberFormatException e) { System.out.println("Enter a valid number."); } }
    }
}
