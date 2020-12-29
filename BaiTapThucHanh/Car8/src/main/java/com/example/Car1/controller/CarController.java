package com.example.Car1.controller;

import java.util.Optional;

import com.example.Car1.model.Car;
import com.example.Car1.repository.CarDao;
import com.example.Car1.request.DeleteRequest;
import com.example.Car1.request.SearchRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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


    @GetMapping("/add")
    public String add(Model model) {
    model.addAttribute("car", new Car()); //Tạo đối tượng Car rỗng để truyền vào th:object="${car}"
    return "form";
    }


 @PostMapping("/save")
public String save(Car car, BindingResult result, RedirectAttributes redirect) {
  if (result.hasErrors()) {
    return "form";
  }
  if (car.getId() > 0) { //Nếu có trường id có nghĩa là đây là edit form
    carDao.update(car);
  } else { //Nếu id ==0 có nghĩa car lần đầu được add new
    carDao.add(car);
  }
      
  return "redirect:/car";
}


    @GetMapping(value = "/edit/{id}")
    public String editCarId(@PathVariable("id") int id, Model model) {    
    Optional<Car> car = carDao.get(id);
    if (car.isPresent()) {
    model.addAttribute("car",car.get());
    } 
    return "form";
    }


    @PostMapping(value = "/delete")
    public String deleteByID(@ModelAttribute DeleteRequest request, BindingResult result) {
    if (!result.hasErrors()) {
    carDao.deleteByID(request.getId());
    }            
    return "redirect:/car";
    }


    @GetMapping("/search") //Hiển thị form tìm kiếm
    public String searchForm(Model model) { 
    model.addAttribute("searchrequest", new SearchRequest());   
    return "search";
    }
  

    @PostMapping("/search")
    public String searchByKeyword(@ModelAttribute SearchRequest request, BindingResult bindingResult, Model model) {
    if (!bindingResult.hasFieldErrors()) {
      model.addAttribute("cars", carDao.searchByKeyword(request.getKeyword()));
    }    
    return "allcars";
} 
 
}

