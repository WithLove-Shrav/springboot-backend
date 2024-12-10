package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import com.example.demo.service.ExamScheduleService;

@RestController
public class ExamAllocationController {

    @Autowired
    private ExamScheduleService examScheduleService;

    @GetMapping("/exam/schedule/{year}")
    public List<Map<String, Object>> getExamSchedule(@PathVariable String year) {
        return examScheduleService.fetchExamTimetableByYear(year); // Return specific year timetable
    }

    @GetMapping("/exam/halls/{year}")
    public List<Map<String, Object>> getExamHalls(@PathVariable String year) {
        return examScheduleService.fetchExamHallsByYear(year); // Return specific year halls details
    }

    @GetMapping("/staff")
    public List<Map<String, Object>> getStaff() {
        return examScheduleService.fetchStaff(); // Return staff details
    }

    @GetMapping("/exam/allocate")
    public Map<String, List<String>> allocateStaffToExamsGet(
            @RequestParam String year,
            @RequestParam int numberOfHalls,
            @RequestParam List<String> selectedHalls) {
        return examScheduleService.allocateStaffToExams(year, numberOfHalls, selectedHalls);
    }




}
