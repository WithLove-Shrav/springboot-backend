package com.example.demo.service;

import com.google.cloud.firestore.Firestore;
import java.util.HashMap;

import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import com.google.firebase.FirebaseApp;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextRefreshedEvent;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExamScheduleService {

    private Firestore db;
    private int roundRobinIndex = 0;

    // Constructor
    public ExamScheduleService(FirebaseApp firebaseApp) {
        // FirebaseApp will be injected by Spring if needed, otherwise handled by EventListener
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeFirestore() {
        try {
            // Ensure Firebase is initialized
            if (FirebaseApp.getApps().isEmpty()) {
                throw new IllegalStateException("FirebaseApp is not initialized.");
            }
            // Initialize Firestore once FirebaseApp is available
            this.db = FirestoreClient.getFirestore();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firestore: " + e.getMessage());
        }
    }

    // Fetch timetable based on year (2 for second year, 3 for third year)
    public List<Map<String, Object>> fetchExamTimetableByYear(String year) {
        String documentId = (year.equals("2")) ? "vwjSqMd102em0OAwNPVy" : "4KwtAFXTh4Ekw6lVzGbN";
        try {
            // Get the exam timetable document by document ID
            DocumentSnapshot documentSnapshot = db.collection("exam_timetable").document(documentId).get().get();
            
            // Debugging: Log the full document data to check its structure
            System.out.println("Fetched Document Data: " + documentSnapshot.getData());

            // Create a list and add the document data to it
            List<Map<String, Object>> examTimetables = new ArrayList<>();
            if (documentSnapshot.exists()) {
                examTimetables.add(documentSnapshot.getData()); // Add the document data as Map to the list
            }
            return examTimetables; // Return the timetable for the year
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fetch exam halls (if needed for specific year)
    public List<Map<String, Object>> fetchExamHallsByYear(String year) {
        return fetchExamHalls();
    }

    public List<Map<String, Object>> fetchExamHalls() {
        try {
            // Fetch data from "exam_halls" collection
            QuerySnapshot querySnapshot = db.collection("exam_halls").get().get();
            List<Map<String, Object>> halls = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                halls.add(document.getData()); // Add the document data as Map to the list
            }
            return halls; // Return the list of exam halls
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fetch staff data with count
    public List<Map<String, Object>> fetchStaff() {
        try {
            // Fetch data from "staff" collection
            QuerySnapshot querySnapshot = db.collection("staff").get().get();
            List<Map<String, Object>> staff = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                staff.add(document.getData()); // Add the document data as Map to the list
            }
            return staff; // Return the list of staff members
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Allocate staff to exams for forenoon and afternoon slots based on new Firestore structure
    public Map<String, List<String>> allocateStaffToExams(String year, int numberOfHalls, List<String> selectedHalls) {
        List<Map<String, Object>> exams = fetchExamTimetableByYear(year); // Fetch timetable for the specified year
        List<Map<String, Object>> allHalls = fetchExamHalls(); // Fetch all available halls
        List<QueryDocumentSnapshot> staff = fetchStaffWithCount(); // Get staff with count data as QueryDocumentSnapshot

        int examCount = 0; // To count valid exams
        List<String> validExams = new ArrayList<>();
        List<String> allocatedExams = new ArrayList<>(); // To track allocated exams
        Map<String, List<String>> dayWiseAllocations = new TreeMap<>(); // Store allocations grouped by day (sorted by day)

        // Keep track of allocated staff IDs
        List<String> allocatedStaffIds = new ArrayList<>();

        // Map to store hall details by their names for quick lookup
        Map<String, Map<String, Object>> hallMap = new HashMap<>();
        for (Map<String, Object> hall : allHalls) {
            hallMap.put((String) hall.get("name"), hall);
        }

        // Validate selected halls count
        if (selectedHalls.size() < numberOfHalls) {
            throw new IllegalArgumentException("Number of selected halls is less than the required number of halls.");
        }

        // Limit the selected halls to the specified number of halls
        List<Map<String, Object>> halls = new ArrayList<>();
        for (int i = 0; i < numberOfHalls; i++) {
            String hallName = selectedHalls.get(i);
            if (hallMap.containsKey(hallName)) {
                halls.add(hallMap.get(hallName));
            } else {
                throw new IllegalArgumentException("Invalid hall selected: " + hallName);
            }
        }

        // Determine Firestore document ID based on year
        String documentId = year.equals("2") ? "vwjSqMd102em0OAwNPVy" : "4KwtAFXTh4Ekw6lVzGbN";

        try {
            // Traverse the Firestore document's fields for each day's exam
            for (Map<String, Object> exam : exams) {
                for (Map.Entry<String, Object> entry : exam.entrySet()) {
                    String examSlot = entry.getKey();
                    String examName = (String) entry.getValue();

                    // Skip "null" or empty exams
                    if (examName != null && !examName.equals("null")) {
                        validExams.add(examName); // Add exam to valid list
                        examCount++;
                    }
                }
            }

            if (examCount == 0) {
                return dayWiseAllocations; // No exams to allocate, return empty map
            }

            int hallIndex = 0; // To iterate through the selected halls

            // Allocate staff for valid exams (forenoon and afternoon slots)
            for (String examName : validExams) {
                String day = "Unknown Day"; // Default value if day is not found
                String slot = ""; // Slot (Forenoon or Afternoon)

                // Match the exam to its day and slot
                for (Map<String, Object> exam : exams) {
                    for (Map.Entry<String, Object> entry : exam.entrySet()) {
                        String examSlotKey = entry.getKey();
                        String examField = examSlotKey.split(" - ")[0]; // "Day 1", "Day 2", etc.
                        String examSlotValue = (String) entry.getValue();

                        if (examSlotValue.equals(examName)) {
                            day = examField; // Set the correct day
                            slot = examSlotKey.contains("Forenoon") ? "Forenoon" : "Afternoon";
                        }
                    }
                }

                // Allocate staff for Forenoon and Afternoon slots
                if (slot.equals("Forenoon") && !allocatedExams.contains(examName)) {
                    QueryDocumentSnapshot forenoonStaffDoc = chooseStaffWithMinCount(staff, allocatedStaffIds);

                    if (forenoonStaffDoc != null) {
                        String forenoonStaffName = (String) forenoonStaffDoc.get("name", String.class);

                        // Fetch selected hall details
                        Map<String, Object> forenoonHallDetails = halls.get(hallIndex % halls.size());
                        String forenoonHallName = (String) forenoonHallDetails.getOrDefault("name", "Unknown Hall");
                        String forenoonHallDimensions = (String) forenoonHallDetails.getOrDefault("dimensions", "(Unknown)");

                        String forenoonStaffId = forenoonStaffDoc.getId();
                        String forenoonAllocation = "Forenoon: " + examName + ": " + forenoonStaffName + ": " 
                                                    + forenoonHallName + " (" + forenoonHallDimensions + ")";

                        // Add allocation to the day-wise map
                        dayWiseAllocations.computeIfAbsent(day, k -> new ArrayList<>()).add(forenoonAllocation);
                        allocatedExams.add(examName); // Mark as allocated

                        // Increment the count of allocated staff in Firestore
                        incrementStaffCount(forenoonStaffId);
                    }
                }

                if (slot.equals("Afternoon") && !allocatedExams.contains(examName)) {
                    QueryDocumentSnapshot afternoonStaffDoc = chooseStaffWithMinCount(staff, allocatedStaffIds);

                    if (afternoonStaffDoc != null) {
                        String afternoonStaffName = (String) afternoonStaffDoc.get("name", String.class);

                        // Fetch selected hall details
                        Map<String, Object> afternoonHallDetails = halls.get(hallIndex % halls.size());
                        String afternoonHallName = (String) afternoonHallDetails.getOrDefault("name", "Unknown Hall");
                        String afternoonHallDimensions = (String) afternoonHallDetails.getOrDefault("dimensions", "(Unknown)");

                        String afternoonAllocation = "Afternoon: " + examName + ": " + afternoonStaffName + ": " 
                                                    + afternoonHallName + " (" + afternoonHallDimensions + ")";

                        String staffId = afternoonStaffDoc.getId();

                        // Add allocation to the day-wise map
                        dayWiseAllocations.computeIfAbsent(day, k -> new ArrayList<>()).add(afternoonAllocation);
                        allocatedExams.add(examName); // Mark as allocated

                        // Increment the count of allocated staff in Firestore
                        incrementStaffCount(staffId);
                    }
                }

                hallIndex++; // Move to the next hall for the next allocation
            }

            // Prepare the JSON structure to write to Firestore
            Map<String, Object> allocationResults = new HashMap<>();
            allocationResults.put("year", year);
            allocationResults.put("allocations", dayWiseAllocations);

            // Write or overwrite the document in the "exam_allocation_results" collection
            db.collection("exam_allocation_results")
                    .document(documentId)
                    .set(allocationResults) // Overwrite if exists, create if not
                    .get(); // Wait for the operation to complete

            System.out.println("Allocation results updated for document ID: " + documentId);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to update allocation results for document ID: " + documentId);
        }

        // Return the day-wise allocations as the response
        return dayWiseAllocations;
    }





    // Fetch staff with their count (stored in Firestore)
    public List<QueryDocumentSnapshot> fetchStaffWithCount() {
        try {
            QuerySnapshot querySnapshot = db.collection("staff").get().get();
            List<QueryDocumentSnapshot> staff = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                staff.add(document); // Add the document itself (not just data) to the list
            }
            System.out.println("Fetched " + staff.size() + " staff members.");
            return staff;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Choose staff with minimum count
    private QueryDocumentSnapshot chooseStaffWithMinCount(List<QueryDocumentSnapshot> staff, List<String> allocatedStaffIds) {
        // Initialize variables to track the staff with the minimum count
        QueryDocumentSnapshot selectedStaff = null;
        double minCount = Double.MAX_VALUE;
        List<QueryDocumentSnapshot> candidates = new ArrayList<>();
        
        // First, find the staff with the minimum count
        for (QueryDocumentSnapshot staffMember : staff) {
            Object countObj = staffMember.get("count");
            double count = (countObj instanceof Number) ? ((Number) countObj).doubleValue() : 0;
            
            if (count < minCount) {
                // Reset the candidates list if a new minimum count is found
                minCount = count;
                candidates.clear();
                candidates.add(staffMember);
            } else if (count == minCount) {
                // If count is equal to minCount, add to candidates list
                candidates.add(staffMember);
            }
        }

        // Now, we have the staff with the minimum count. Remove already allocated staff from the list.
        List<QueryDocumentSnapshot> remainingCandidates = new ArrayList<>();
        for (QueryDocumentSnapshot candidate : candidates) {
            if (!allocatedStaffIds.contains(candidate.getId())) {
                remainingCandidates.add(candidate);  // Only add staff who have not been allocated yet
            }
        }

        // Ensure roundRobinIndex is within bounds of remainingCandidates size
        if (!remainingCandidates.isEmpty()) {
            // Round-robin logic: Cycle through the remaining candidates in a round-robin manner
            selectedStaff = remainingCandidates.get(roundRobinIndex % remainingCandidates.size());  // Use modulo to wrap around

            // Mark this staff as allocated
            allocatedStaffIds.add(selectedStaff.getId());

            // Increment and wrap around the index correctly
            roundRobinIndex = (roundRobinIndex + 1) % remainingCandidates.size();
        } else {
            // If no remaining candidates, allocate from the entire staff list again (circular)
            for (QueryDocumentSnapshot staffMember : staff) {
                if (!allocatedStaffIds.contains(staffMember.getId())) {
                    selectedStaff = staffMember;  // Allocate the next available staff
                    allocatedStaffIds.add(selectedStaff.getId());
                    break;  // Once allocated, break from the loop
                }
            }
        }

        return selectedStaff;
    }







    // Increment the count of staff in Firestore
    private void incrementStaffCount(String staffId) {
        try {
            db.collection("staff").document(staffId)
                .update("count", FieldValue.increment(1)); // Increment the count by 1
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to increment staff count.");
        }
    }
}
