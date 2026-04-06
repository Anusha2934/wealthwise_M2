package com.wealthwise.controller;

import com.wealthwise.entity.SchemeCategory;
import com.wealthwise.entity.SchemeMaster;
import com.wealthwise.repository.SchemeCategoryRepository;
import com.wealthwise.repository.SchemeMasterRepository;
import com.wealthwise.service.SchemeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M02 — Scheme Master REST Controller
 *
 * Base URL: /api/v1/schemes
 */
@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SchemeController {

    private final SchemeMasterRepository   schemeRepo;
    private final SchemeCategoryRepository categoryRepo;
    private final SchemeCategoryService    categoryService;

    // GET /api/v1/schemes?page=0&size=20&amc=HDFC
    @GetMapping
    public Page<SchemeMaster> list(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String amc) {

        var pageable = PageRequest.of(page, size);
        return (amc != null && !amc.isBlank())
            ? schemeRepo.findByAmcNameContainingIgnoreCase(amc, pageable)
            : schemeRepo.findAll(pageable);
    }

    // GET /api/v1/schemes/{amfiCode}
    @GetMapping("/{amfiCode}")
    public ResponseEntity<SchemeMaster> get(@PathVariable String amfiCode) {
        return schemeRepo.findByAmfiCode(amfiCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/v1/schemes/{amfiCode}/category
    @GetMapping("/{amfiCode}/category")
    public ResponseEntity<SchemeCategory> getCategory(@PathVariable String amfiCode) {
        return categoryRepo.findByAmfiCode(amfiCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/v1/schemes/summary
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total",          schemeRepo.count());
        map.put("active",         schemeRepo.countByIsActive(true));
        map.put("inactive",       schemeRepo.countByIsActive(false));
        map.put("directPlans",    schemeRepo.countByPlanType(SchemeMaster.PlanType.DIRECT));
        map.put("regularPlans",   schemeRepo.countByPlanType(SchemeMaster.PlanType.REGULAR));
        return map;
    }

    // GET /api/v1/schemes/category/EQUITY
    @GetMapping("/category/{broad}")
    public List<SchemeCategory> byBroadCategory(
            @PathVariable String broad) {
        return categoryRepo.findByBroadCategory(
            SchemeCategory.BroadCategory.valueOf(broad.toUpperCase()));
    }

    // POST /api/v1/schemes/classify-all  (admin only — run after seeding)
    @PostMapping("/classify-all")
    public ResponseEntity<String> classifyAll() {
        categoryService.classifyAll();
        return ResponseEntity.ok("Classification complete for "
            + categoryRepo.count() + " schemes.");
    }
}
