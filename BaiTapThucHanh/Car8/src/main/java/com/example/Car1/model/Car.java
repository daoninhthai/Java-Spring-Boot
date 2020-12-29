package com.example.Car1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Car {
    @JsonIgnore
        int id ;
       String introduce;
        String title;
        String description;
        String imglink;

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


        public boolean matchWithKeyword(String keyword) {
            String keywordLowerCase = keyword.toLowerCase();
            return (title.toLowerCase().contains(keywordLowerCase) || 
            description.toLowerCase().contains(keywordLowerCase));
         }

         public String getImglink() {
             return imglink;
         }

         public void setImglink(String imglink) {
             this.imglink = imglink;
         }

         public String getIntroduce() {
             return introduce;
         }

         public void setIntroduce(String introduce) {
             this.introduce = introduce;
         }
       
        
}

  
