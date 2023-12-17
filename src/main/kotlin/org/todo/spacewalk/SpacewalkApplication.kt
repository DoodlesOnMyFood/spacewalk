package org.todo.spacewalk

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpacewalkApplication

fun main(args: Array<String>) {
    runApplication<SpacewalkApplication>(*args)
}
