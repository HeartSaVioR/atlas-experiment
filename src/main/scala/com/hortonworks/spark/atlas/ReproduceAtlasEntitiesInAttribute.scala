/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.spark.atlas

import java.util.UUID

import com.hortonworks.spark.atlas.utils.Logging
import org.apache.atlas.AtlasConstants
import org.apache.atlas.model.instance.AtlasEntity
import org.apache.spark.sql.kafka010.atlas.KafkaTopicInformation

import scala.util.Random

class ReproduceAtlasEntitiesInAttribute extends Logging {
  val uuid = UUID.randomUUID().toString

  val atlasClientConf = new AtlasClientConf
  val atlasClient = AtlasClient.atlasClient(atlasClientConf)

  val defaultClusterName = "default"
  val clusterName = "reproduce"
  val currentUser = "reproducer"

  val rand = new Random()

  val inputTopics = Seq(
    KafkaTopicInformation(createUniqueName("sourceTopic1"), Some(clusterName)),
    KafkaTopicInformation(createUniqueName("sourceTopic2"), Some(clusterName)),
    KafkaTopicInformation(createUniqueName("sourceTopic3"), Some(clusterName))
  )

  val outputTopic = KafkaTopicInformation(createUniqueName("sinkTopic"), Some(clusterName))

  private def testNoCacheOnCreatedEntities(): Unit = {
    logInfo("Reproducing case on Kafka -> Kafka streaming query - without caching entities...")
    logInfo(s"UUID: $uuid")

    def runBatch(batchId: Long, inputTopics: Seq[KafkaTopicInformation], outputTopics: Seq[KafkaTopicInformation]): Unit = {
      logInfo(s"Emulating batch $batchId: $inputTopics -> $outputTopics")
      val entities = createEntities(inputTopics, outputTopics)
      logInfo(s"Entities: $entities")
      atlasClient.createEntities(entities)
    }

    val outputTopics = Seq(outputTopic)

    // batch 1
    val inputTopicsBatch1 = Seq(inputTopics.head)
    runBatch(1, inputTopicsBatch1, outputTopics)

    // batch 2
    val inputTopicsBatch2 = Seq(inputTopics(1))
    runBatch(2, inputTopicsBatch2, outputTopics)

    // batch 3
    val inputTopicsBatch3 = Seq(inputTopics(1), inputTopics(2))
    runBatch(3, inputTopicsBatch3, outputTopics)

    // batch 4
    val inputTopicsBatch4 = Seq(inputTopics(2))
    runBatch(4, inputTopicsBatch4, outputTopics)

    logInfo("Done...")
  }

  private def createEntities(inputKafkaTopics: Seq[KafkaTopicInformation],
                             outputKafkaTopics: Seq[KafkaTopicInformation]): Seq[AtlasEntity] = {
    val inputEntities = inputKafkaTopics.flatMap(createKafkaEntity).toList
    val outputEntities = outputKafkaTopics.flatMap(createKafkaEntity).toList
    val processEntity = createProcessEntity(inputEntities, outputEntities)

    Seq(processEntity) ++ inputEntities ++ outputEntities
  }

  private def createKafkaEntity(topicInfo: KafkaTopicInformation): Seq[AtlasEntity] = {
    kafkaToEntity(defaultClusterName, topicInfo)
  }

  private def createProcessEntity(inputTopicEntities: List[AtlasEntity],
                                  outputTopicEntities: List[AtlasEntity]): AtlasEntity = {
    val logMap = Map(
      "executionId" -> rand.nextLong().toString,
      "remoteUser" -> "testUser",
      "executionTime" -> System.currentTimeMillis().toString,
      "details" -> "dummy execution",
      "sparkPlanDescription" -> "dummy physical plan")

    etlProcessToEntity(inputTopicEntities, outputTopicEntities, logMap)
  }

  private def kafkaToEntity(cluster: String, topic: KafkaTopicInformation): Seq[AtlasEntity] = {
    val KAFKA_TOPIC_STRING = "kafka_topic"

    val topicName = topic.topicName.toLowerCase
    val clusterName = topic.clusterName match {
      case Some(customName) => customName
      case None => cluster
    }

    val kafkaEntity = new AtlasEntity(KAFKA_TOPIC_STRING)
    kafkaEntity.setAttribute("qualifiedName", topicName + '@' + clusterName)
    kafkaEntity.setAttribute("name", topicName)
    kafkaEntity.setAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, clusterName)
    kafkaEntity.setAttribute("uri", topicName)
    kafkaEntity.setAttribute("topic", topicName)
    Seq(kafkaEntity)
  }

  private def etlProcessToEntity(
      inputs: List[AtlasEntity],
      outputs: List[AtlasEntity],
      logMap: Map[String, String]): AtlasEntity = {
    import scala.collection.JavaConverters._

    val PROCESS_TYPE_STRING = "spark_process"

    val entity = new AtlasEntity(PROCESS_TYPE_STRING)

    val appId = uuid
    val appName = s"Reproduced Spark Job $appId"
    entity.setAttribute("qualifiedName", appId)
    entity.setAttribute("name", appName)
    entity.setAttribute("currUser", currentUser)
    entity.setAttribute("inputs", inputs.asJava)  // Dataset and Model entity
    entity.setAttribute("outputs", outputs.asJava)  // Dataset entity
    logMap.foreach { case (k, v) => entity.setAttribute(k, v)}
    entity
  }

  private def createUniqueName(name: String): String = {
    s"${name}_$uuid"
  }
}

object ReproduceAtlasEntitiesInAttribute {
  def main(args: Array[String]): Unit = {
    val mainClass = new ReproduceAtlasEntitiesInAttribute
    mainClass.testNoCacheOnCreatedEntities()
  }

}