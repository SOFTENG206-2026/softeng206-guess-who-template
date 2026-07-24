package nz.ac.auckland.model;

public class Customer {

  private String name;
  private String profession;
  private String image;

  public Customer(String name, String profession, String image) {
    this.name = name;
    this.profession = profession;
    this.image = image;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProfession() {
    return profession;
  }

  public void setProfession(String profession) {
    this.profession = profession;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }
}
