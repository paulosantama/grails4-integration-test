package integration.test


import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

@Integration
@Rollback
class NotebookSpec extends Specification {

    def "save the notebook and retrieve consistent information"() {
        given:
        String owner = 'test owner'
        String subject = 'test subject'
        Map<String, Object> extraProperties = [
                one  : 'two',
                three: 'four',
                five : 'six'
        ]

        when:
        Long id = new Notebook(
                owner: owner,
                subject: subject,
                extraProperties: extraProperties
        ).save(flush: true, failOnError: true)?.id

        then:
        Notebook retrieved = Notebook.findById(id)
        retrieved?.owner == owner
        retrieved?.subject == subject
        retrieved?.extraProperties?.equals([*: extraProperties])
    }
}
