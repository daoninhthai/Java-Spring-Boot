package com.example.Car1.repository;

import java.util.List;
import java.util.Optional;

import com.example.Car1.model.Car;

public class CarDao extends Dao<Car> {

    public CarDao(){
        collections.add(new Car(1, "honda","aa"));
        collections.add(new Car(2, "yamaha","bb"));

    }
    @Override
    public List<Car> getAll() {
        
        return collections;
    }

    @Override
    public void add(Car t) {
      

    }

    @Override
    public void delete(Car t) {
        

    }

    @Override
    public void deleteByID(int id) {
       

    }

    @Override
    public Optional<Car> get(int id) {
       
        return null;
    }

 

    @Override
    public void update(Car t) {
       

    }

   
}
