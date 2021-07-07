package integration.test

import grails.persistence.Entity

@Entity
class Book {

    String name
    String author

    static constraints = {
        name nullable: false
        author nullable: false
    }

}
