package com.example.Car1.model;

public class Car {
    
        int id ;
        String title;
        String description;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Car(int id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }

        
}

  
