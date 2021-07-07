package integration.test

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

@Integration
@Rollback
class BookSpec extends Specification {

    def "save the book and retrieve consistent information" () {
        given:
        String name = 'test name'
        String author = 'test author'

        Book book = new Book(
                name: name,
                author: author
        )

        when:
        book.save(flush: true, failOnError: true)

        then:
        Book retrieved = Book.findById(book.id)
        retrieved.name == name
        retrieved.author == author
    }

}
