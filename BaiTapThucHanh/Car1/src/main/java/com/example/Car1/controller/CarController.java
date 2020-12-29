package com.example.Car1.controller;

import com.example.Car1.repository.CarDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/car")
public class CarController {
    @Autowired
    private CarDao carDao;
    @GetMapping
    public String listAll(Model model){
        model.addAttribute("cars",carDao.getAll());
        return "allcars";
    }
}
