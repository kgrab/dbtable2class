package com.datawizards.dbtable2class

import java.io.File
import java.sql.DriverManager

import com.datawizards.dbtable2class.dialects.H2Dialect
import com.datawizards.dbtable2class.model.TableClassMapping
import org.scalatest._

class ClassGeneratorTest extends FunSuite with Matchers {

  private val url = "jdbc:h2:mem:test"
  private val connection = DriverManager.getConnection(url, "", "")

  test("generate people") {
    connection.createStatement().execute("create table PEOPLE(NAME VARCHAR, AGE INT)")
    val classDefinition = ClassGenerator.generateClass("Person", url, null, "PUBLIC", "PEOPLE", H2Dialect)
    classDefinition.replace("\n","").replace("\r","") should equal(
      """
        |case class Person(
        |  NAME: String,
        |  AGE: Int
        |)""".stripMargin.replace("\n","").replace("\r","")
    )
  }

  test("generate all types") {
    val ddl =
      """create table ALL_TYPES(
        |STRVAL	VARCHAR,
        |INTVAL	INTEGER,
        |LONGVAL	BIGINT,
        |DOUBLEVAL	DOUBLE,
        |FLOATVAL	REAL,
        |SHORTVAL	SMALLINT,
        |BOOLEANVAL	BOOLEAN,
        |BYTEVAL	TINYINT,
        |DATEVAL	DATE,
        |TIMESTAMPVAL	TIMESTAMP
        |)
      """.stripMargin
    connection.createStatement().execute(ddl)
    val classDefinition = ClassGenerator.generateClass("AllTypes", url, null, "PUBLIC", "ALL_TYPES", H2Dialect)
    classDefinition.replace("\n","").replace("\r","") should equal(
      """
        |case class AllTypes(
        |  STRVAL: String,
        |  INTVAL: Int,
        |  LONGVAL: Long,
        |  DOUBLEVAL: Double,
        |  FLOATVAL: Float,
        |  SHORTVAL: Short,
        |  BOOLEANVAL: Boolean,
        |  BYTEVAL: Byte,
        |  DATEVAL: java.sql.Date,
        |  TIMESTAMPVAL: java.sql.Timestamp
        |)""".stripMargin.replace("\n","").replace("\r","")
    )
  }

  test("Generate multiple classes") {
    connection.createStatement().execute("create table T1(NAME VARCHAR, AGE INT)")
    connection.createStatement().execute("create table T2(TITLE VARCHAR, AUTHOR VARCHAR)")
    val classDefinitions = ClassGenerator.generateClasses(
      url, null, H2Dialect, Seq(
        TableClassMapping("PUBLIC", "T1", "", "Person"),
        TableClassMapping("PUBLIC", "T2", "", "Book")
      )
    )
    classDefinitions.map(_.replace("\n","").replace("\r","")) should equal(Seq(
      """
        |case class Person(
        |  NAME: String,
        |  AGE: Int
        |)""".stripMargin.replace("\n","").replace("\r",""),
      """
        |case class Book(
        |  TITLE: String,
        |  AUTHOR: String
        |)""".stripMargin.replace("\n","").replace("\r","")
      )
    )
  }

  test("Generate to directory") {
    connection.createStatement().execute("create table T11(NAME VARCHAR, AGE INT)")
    connection.createStatement().execute("create table T22(TITLE VARCHAR, AUTHOR VARCHAR)")
    ClassGenerator.generateClassesToDirectory(
      "target", url, null, H2Dialect, Seq(
        TableClassMapping("PUBLIC", "T11", "com.datawizards.model", "Person"),
        TableClassMapping("PUBLIC", "T22", "com.datawizards.model", "Book")
      )
    )

    deleteDirectory("target/com")

    readFileContent("target/com/datawizards/model/Person.scala").replace("\n","").replace("\r","") should equal(
      """package com.datawizards.model
        |
        |case class Person(
        |  NAME: String,
        |  AGE: Int
        |)""".stripMargin.replace("\n","").replace("\r","")
    )
    readFileContent("target/com/datawizards/model/Book.scala").replace("\n","").replace("\r","") should equal(
      """package com.datawizards.model
        |
        |case class Book(
        |  TITLE: String,
        |  AUTHOR: String
        |)""".stripMargin.replace("\n","").replace("\r","")
    )
  }

  private def readFileContent(file: String): String =
    scala.io.Source.fromFile(file).getLines().mkString("\n")

  private def deleteDirectory(dir: String): Unit =
    new File(dir).delete()
}
