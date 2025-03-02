# CECS 445 - [Open App](https://cecs-445-ems.herokuapp.com/)

## Running Locally

Make sure you have [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [Maven](http://maven.apache.org/download.html) installed.  Also, install the [Heroku CLI](https://cli.heroku.com/).

```sh
$ git clone https://github.com/MrBmikhael/CECS-445.git
$ cd CECS-445
$ mvn clean install
$ heroku local web
```

Your app should now be running on [localhost:5000](http://localhost:5000/).

## Tools Used

- [Spring Framework](http://spring.io/)
- [Bootstrap v3.3.7](https://getbootstrap.com/)
- [jQuery v3.3.1](https://jquery.com/)


## Documentation

For more information about using Java on Heroku, see these Dev Center articles:

- [Java on Heroku](https://devcenter.heroku.com/categories/java)
