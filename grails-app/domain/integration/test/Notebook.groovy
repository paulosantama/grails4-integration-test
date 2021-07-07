package integration.test

import grails.gorm.annotation.Entity
import integration.test.hibernate.ParameterizedJsonbUserType

@Entity
class Notebook {

    String owner
    String subject
    Map<String, Object> extraProperties = [:]

    static constraints = {
        owner nullable: false
        subject nullable: false
    }

    static mapping = {
        extraProperties type: ParameterizedJsonbUserType, params: [type: Map]
    }

}
