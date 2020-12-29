package com.example.Car1.controller;

import java.util.Optional;

import com.example.Car1.model.Car;
import com.example.Car1.repository.CarDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @GetMapping(value = "/{id}")
    public String getByID(@PathVariable("id") int id, Model model) {
    Optional<Car> car = carDao.get(id);
    if (car.isPresent()) {
        model.addAttribute("car", car.get());
    }    
        return "car";
}
}

