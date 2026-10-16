package com.dbtools.controller;

import com.dbtools.service.DatabaseConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final DatabaseConfigService configService;

    public PageController(DatabaseConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("databases", configService.listDatabases());
        return "index";
    }
}
