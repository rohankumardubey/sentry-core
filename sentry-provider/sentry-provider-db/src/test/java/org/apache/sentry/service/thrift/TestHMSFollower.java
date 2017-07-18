/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sentry.service.thrift;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.NotificationEvent;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hive.hcatalog.messaging.HCatEventMessage;
import org.apache.hive.hcatalog.messaging.HCatEventMessage.EventType;
import org.apache.sentry.binding.metastore.messaging.json.SentryJSONMessageFactory;
import org.apache.sentry.hdfs.Updateable;
import org.apache.sentry.provider.db.service.persistent.SentryStore;
import org.apache.sentry.provider.db.service.thrift.TSentryAuthorizable;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.security.auth.login.LoginException;

public class TestHMSFollower {

  private final static String hiveInstance = "server2";
  private final static Configuration configuration = new Configuration();
  private final SentryJSONMessageFactory messageFactory = new SentryJSONMessageFactory();
  private final SentryStore sentryStore = Mockito.mock(SentryStore.class);
  private static HiveSimpleConnectionFactory hiveConnectionFactory;

  @BeforeClass
  public static void setup() throws IOException, LoginException {
    hiveConnectionFactory = new HiveSimpleConnectionFactory(configuration, new HiveConf());
    hiveConnectionFactory.init();
    configuration.set("sentry.hive.sync.create", "true");
  }

  /**
   * Constructs create database event and makes sure that appropriate sentry store API's
   * are invoke when the event is processed by hms follower.
   *
   * @throws Exception
   */
  @Test
  public void testCreateDatabase() throws Exception {
    String dbName = "db1";

    // Create notification events
    NotificationEvent notificationEvent = new NotificationEvent(1, 0,
        HCatEventMessage.EventType.CREATE_DATABASE.toString(),
        messageFactory.buildCreateDatabaseMessage(new Database(dbName, null, "hdfs:///db1", null))
            .toString());
    List<NotificationEvent> events = new ArrayList<>();
    events.add(notificationEvent);
    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    hmsFollower.processNotifications(events);

    TSentryAuthorizable authorizable = new TSentryAuthorizable(hiveInstance);
    authorizable.setServer(hiveInstance);
    authorizable.setDb("db1");

    verify(sentryStore, times(1))
        .dropPrivilege(authorizable, NotificationProcessor.getPermUpdatableOnDrop(authorizable));
  }

  /**
   * Constructs drop database event and makes sure that appropriate sentry store API's
   * are invoke when the event is processed by hms follower.
   *
   * @throws Exception
   */
  @Test
  public void testDropDatabase() throws Exception {
    String dbName = "db1";

    // Create notification events
    NotificationEvent notificationEvent = new NotificationEvent(1, 0,
        HCatEventMessage.EventType.DROP_DATABASE.toString(),
        messageFactory.buildDropDatabaseMessage(new Database(dbName, null, "hdfs:///db1", null))
            .toString());
    List<NotificationEvent> events = new ArrayList<>();
    events.add(notificationEvent);

    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    hmsFollower.processNotifications(events);

    TSentryAuthorizable authorizable = new TSentryAuthorizable(hiveInstance);
    authorizable.setServer(hiveInstance);
    authorizable.setDb("db1");

    verify(sentryStore, times(1))
        .dropPrivilege(authorizable, NotificationProcessor.getPermUpdatableOnDrop(authorizable));
  }

  /**
   * Constructs create table event and makes sure that appropriate sentry store API's
   * are invoke when the event is processed by hms follower.
   *
   * @throws Exception
   */
  @Test
  public void testCreateTable() throws Exception {
    String dbName = "db1";
    String tableName = "table1";

    // Create notification events
    StorageDescriptor sd = new StorageDescriptor();
    sd.setLocation("hdfs:///db1.db/table1");
    NotificationEvent notificationEvent = new NotificationEvent(1, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(
            new Table(tableName, dbName, null, 0, 0, 0, sd, null, null, null, null, null))
            .toString());
    List<NotificationEvent> events = new ArrayList<>();
    events.add(notificationEvent);

    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    hmsFollower.processNotifications(events);

    TSentryAuthorizable authorizable = new TSentryAuthorizable(hiveInstance);
    authorizable.setServer(hiveInstance);
    authorizable.setDb("db1");
    authorizable.setTable(tableName);

    verify(sentryStore, times(1))
        .dropPrivilege(authorizable, NotificationProcessor.getPermUpdatableOnDrop(authorizable));
  }

  /**
   * Constructs drop table event and makes sure that appropriate sentry store API's
   * are invoke when the event is processed by hms follower.
   *
   * @throws Exception
   */
  @Test
  public void testDropTable() throws Exception {
    String dbName = "db1";
    String tableName = "table1";

    // Create notification events
    StorageDescriptor sd = new StorageDescriptor();
    sd.setLocation("hdfs:///db1.db/table1");
    NotificationEvent notificationEvent = new NotificationEvent(1, 0,
        HCatEventMessage.EventType.DROP_TABLE.toString(),
        messageFactory.buildDropTableMessage(
            new Table(tableName, dbName, null, 0, 0, 0, sd, null, null, null, null, null))
            .toString());
    List<NotificationEvent> events = new ArrayList<>();
    events.add(notificationEvent);

    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    hmsFollower.processNotifications(events);

    TSentryAuthorizable authorizable = new TSentryAuthorizable(hiveInstance);
    authorizable.setServer(hiveInstance);
    authorizable.setDb("db1");
    authorizable.setTable(tableName);

    verify(sentryStore, times(1))
        .dropPrivilege(authorizable, NotificationProcessor.getPermUpdatableOnDrop(authorizable));
  }

  /**
   * Constructs rename table event and makes sure that appropriate sentry store API's
   * are invoke when the event is processed by hms follower.
   *
   * @throws Exception
   */
  @Test
  public void testRenameTable() throws Exception {
    String dbName = "db1";
    String tableName = "table1";

    String newDbName = "db1";
    String newTableName = "table2";

    // Create notification events
    StorageDescriptor sd = new StorageDescriptor();
    sd.setLocation("hdfs:///db1.db/table1");
    NotificationEvent notificationEvent = new NotificationEvent(1, 0,
        HCatEventMessage.EventType.ALTER_TABLE.toString(),
        messageFactory.buildAlterTableMessage(
            new Table(tableName, dbName, null, 0, 0, 0, sd, null, null, null, null, null),
            new Table(newTableName, newDbName, null, 0, 0, 0, sd, null, null, null, null, null))
            .toString());
    notificationEvent.setDbName(newDbName);
    notificationEvent.setTableName(newTableName);
    List<NotificationEvent> events = new ArrayList<>();
    events.add(notificationEvent);

    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    hmsFollower.processNotifications(events);

    TSentryAuthorizable authorizable = new TSentryAuthorizable(hiveInstance);
    authorizable.setServer(hiveInstance);
    authorizable.setDb(dbName);
    authorizable.setTable(tableName);

    TSentryAuthorizable newAuthorizable = new TSentryAuthorizable(hiveInstance);
    authorizable.setServer(hiveInstance);
    newAuthorizable.setDb(newDbName);
    newAuthorizable.setTable(newTableName);

    verify(sentryStore, times(1)).renamePrivilege(authorizable, newAuthorizable,
        NotificationProcessor.getPermUpdatableOnRename(authorizable, newAuthorizable));
  }


  @Ignore
  /**
   * Constructs a bunch of events and passed to processor of hms follower. One of those is alter
   * partition event with out actually changing anything(invalid event). Idea is to make sure that
   * hms follower calls appropriate sentry store API's for the events processed by hms follower
   * after processing the invalid alter partition event.
   *
   * @throws Exception
   */
  @Test
  public void testAlterPartitionWithInvalidEvent() throws Exception {
    String dbName = "db1";
    String tableName1 = "table1";
    String tableName2 = "table2";
    long inputEventId = 1;
    List<NotificationEvent> events = new ArrayList<>();
    NotificationEvent notificationEvent;
    List<FieldSchema> partCols;
    StorageDescriptor sd;
    Mockito.doNothing().when(sentryStore).persistLastProcessedNotificationID(Mockito.anyLong());
    //noinspection unchecked
    Mockito.doNothing().when(sentryStore).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));

    Configuration configuration = new Configuration();
    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    // Create a table
    sd = new StorageDescriptor();
    sd.setLocation("hdfs://db1.db/table1");
    partCols = new ArrayList<>();
    partCols.add(new FieldSchema("ds", "string", ""));
    Table table = new Table(tableName1, dbName, null, 0, 0, 0, sd, partCols, null, null, null,
        null);
    notificationEvent = new NotificationEvent(inputEventId, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(table).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName1);
    events.add(notificationEvent);
    inputEventId += 1;
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that addAuthzPathsMapping was invoked once to handle CREATE_TABLE notification
    // and persistLastProcessedNotificationID was not invoked.
    //noinspection unchecked
    verify(sentryStore, times(1)).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(0)).persistLastProcessedNotificationID(Mockito.anyLong());
    reset(sentryStore);
    events.clear();

    // Create a partition
    List<Partition> partitions = new ArrayList<>();
    StorageDescriptor invalidSd = new StorageDescriptor();
    invalidSd.setLocation(null);
    Partition partition = new Partition(Collections.singletonList("today"), dbName, tableName1,
        0, 0, sd, null);
    partitions.add(partition);
    notificationEvent = new NotificationEvent(inputEventId, 0, EventType.ADD_PARTITION.toString(),
        messageFactory.buildAddPartitionMessage(table, partitions).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName1);
    events.add(notificationEvent);
    inputEventId += 1;
    //Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that addAuthzPathsMapping was invoked once to handle ADD_PARTITION notification
    // and persistLastProcessedNotificationID was not invoked.
    //noinspection unchecked
    verify(sentryStore, times(1)).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(0)).persistLastProcessedNotificationID(Mockito.anyLong());
    reset(sentryStore);
    events.clear();

    // Create a alter notification with out actually changing anything.
    // This is an invalid event and should be processed by sentry store.
    // Event Id should be explicitly persisted using persistLastProcessedNotificationID
    notificationEvent = new NotificationEvent(inputEventId, 0, EventType.ALTER_PARTITION.toString(),
        messageFactory.buildAlterPartitionMessage(partition, partition).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName1);
    events.add(notificationEvent);
    inputEventId += 1;
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that persistLastProcessedNotificationID is invoked explicitly.
    verify(sentryStore, times(1)).persistLastProcessedNotificationID(inputEventId - 1);
    reset(sentryStore);
    events.clear();

    // Create a alter notification with some actual change.
    sd = new StorageDescriptor();
    sd.setLocation("hdfs://user/hive/warehouse/db1.db/table1");
    Partition updatedPartition = new Partition(partition);
    updatedPartition.setSd(sd);
    notificationEvent = new NotificationEvent(inputEventId, 0, EventType.ALTER_PARTITION.toString(),
        messageFactory.buildAlterPartitionMessage(partition, updatedPartition).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName1);
    events.add(notificationEvent);
    inputEventId += 1;
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that updateAuthzPathsMapping was invoked once to handle ALTER_PARTITION
    // notification and persistLastProcessedNotificationID was not invoked.
    verify(sentryStore, times(1)).updateAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(0)).persistLastProcessedNotificationID(inputEventId - 1);
    reset(sentryStore);
    events.clear();

    // Create a table
    sd = new StorageDescriptor();
    sd.setLocation("hdfs://db1.db/table2");
    partCols = new ArrayList<>();
    partCols.add(new FieldSchema("ds", "string", ""));
    Table table1 = new Table(tableName2, dbName, null, 0, 0, 0, sd, partCols, null, null, null,
        null);
    notificationEvent = new NotificationEvent(inputEventId, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(table1).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName2);
    events.add(notificationEvent);
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that addAuthzPathsMapping was invoked once to handle CREATE_TABLE notification
    // and persistLastProcessedNotificationID was not invoked.
    //noinspection unchecked
    verify(sentryStore, times(1)).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(0)).persistLastProcessedNotificationID(Mockito.anyLong());
  }

  /**
   * Constructs a bunch of events and passed to processor of hms follower. One of those is alter
   * table event with out actually changing anything(invalid event). Idea is to make sure that
   * hms follower calls appropriate sentry store API's for the events processed by hms follower
   * after processing the invalid alter table event.
   *
   * @throws Exception
   */
  @Test
  public void testAlterTableWithInvalidEvent() throws Exception {
    String dbName = "db1";
    String tableName1 = "table1";
    String tableName2 = "table2";
    long inputEventId = 1;
    List<NotificationEvent> events = new ArrayList<>();
    NotificationEvent notificationEvent;
    List<FieldSchema> partCols;
    StorageDescriptor sd;
    Mockito.doNothing().when(sentryStore).persistLastProcessedNotificationID(Mockito.anyLong());
    //noinspection unchecked
    Mockito.doNothing().when(sentryStore).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));

    Configuration configuration = new Configuration();
    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);

    // Create a table
    sd = new StorageDescriptor();
    sd.setLocation("hdfs://db1.db/table1");
    partCols = new ArrayList<>();
    partCols.add(new FieldSchema("ds", "string", ""));
    Table table = new Table(tableName1, dbName, null, 0, 0, 0, sd, partCols, null, null, null,
        null);
    notificationEvent = new NotificationEvent(inputEventId, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(table).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName1);
    events.add(notificationEvent);
    inputEventId += 1;
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that addAuthzPathsMapping was invoked once to handle CREATE_TABLE notification
    // and persistLastProcessedNotificationID was not invoked.
    //noinspection unchecked
    verify(sentryStore, times(1)).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(0)).persistLastProcessedNotificationID(Mockito.anyLong());
    reset(sentryStore);
    events.clear();

    // Create alter table notification with out actually changing anything.
    // This notification should not be processed by sentry server
    // Notification should be persisted explicitly
    notificationEvent = new NotificationEvent(1, 0,
        HCatEventMessage.EventType.ALTER_TABLE.toString(),
        messageFactory.buildAlterTableMessage(
            new Table(tableName1, dbName, null, 0, 0, 0, sd, null, null, null, null, null),
            new Table(tableName1, dbName, null, 0, 0, 0, sd, null, null, null, null, null))
            .toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName1);
    events = new ArrayList<>();
    events.add(notificationEvent);
    inputEventId += 1;
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that renameAuthzObj and deleteAuthzPathsMapping were  not invoked
    // to handle CREATE_TABLE notification
    // and persistLastProcessedNotificationID is explicitly invoked
    verify(sentryStore, times(0)).renameAuthzObj(Mockito.anyString(), Mockito.anyString(),
        Mockito.any(Updateable.Update.class));
    //noinspection unchecked
    verify(sentryStore, times(0)).deleteAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(1)).persistLastProcessedNotificationID(Mockito.anyLong());
    reset(sentryStore);
    events.clear();

    // Create a table
    sd = new StorageDescriptor();
    sd.setLocation("hdfs://db1.db/table2");
    partCols = new ArrayList<>();
    partCols.add(new FieldSchema("ds", "string", ""));
    Table table1 = new Table(tableName2, dbName, null, 0, 0, 0, sd, partCols, null, null, null,
        null);
    notificationEvent = new NotificationEvent(inputEventId, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(table1).toString());
    notificationEvent.setDbName(dbName);
    notificationEvent.setTableName(tableName2);
    events.add(notificationEvent);
    // Process the notification
    hmsFollower.processNotifications(events);
    // Make sure that addAuthzPathsMapping was invoked once to handle CREATE_TABLE notification
    // and persistLastProcessedNotificationID was not invoked.
    //noinspection unchecked
    verify(sentryStore, times(1)).addAuthzPathsMapping(Mockito.anyString(),
        Mockito.anyCollection(), Mockito.any(Updateable.Update.class));
    verify(sentryStore, times(0)).persistLastProcessedNotificationID(Mockito.anyLong());
  }

  /**
   * Constructs a two events and passed to processor of hms follower. First one being create table
   * event with location information(Invalid Event). Idea is to make sure that hms follower calls
   * appropriate sentry store API's for the event processed by hms follower after processing the
   * invalid create table event.
   *
   * @throws Exception
   */
  public void testCreateTableAfterInvalidEvent() throws Exception {
    String dbName = "db1";
    String tableName = "table1";
    long inputEventId = 1;

    Mockito.doNothing().when(sentryStore).persistLastProcessedNotificationID(Mockito.anyLong());
    //noinspection unchecked
    Mockito.doNothing().when(sentryStore)
        .addAuthzPathsMapping(Mockito.anyString(), Mockito.anyCollection(),
            Mockito.any(Updateable.Update.class));

    // Create invalid notification event. The location of the storage descriptor is null, which is invalid for creating table
    StorageDescriptor invalidSd = new StorageDescriptor();
    invalidSd.setLocation(null);
    NotificationEvent invalidNotificationEvent = new NotificationEvent(inputEventId, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(
            new Table(tableName, dbName, null, 0, 0, 0, invalidSd, null, null, null, null, null))
            .toString());

    // Create valid notification event
    StorageDescriptor sd = new StorageDescriptor();
    sd.setLocation("hdfs://db1.db/table1");
    inputEventId += 1;
    NotificationEvent notificationEvent = new NotificationEvent(inputEventId, 0,
        HCatEventMessage.EventType.CREATE_TABLE.toString(),
        messageFactory.buildCreateTableMessage(
            new Table(tableName, dbName, null, 0, 0, 0, sd, null, null, null, null, null))
            .toString());
    List<NotificationEvent> events = new ArrayList<>();
    events.add(invalidNotificationEvent);
    events.add(notificationEvent);

    Configuration configuration = new Configuration();
    HMSFollower hmsFollower = new HMSFollower(configuration, sentryStore, null,
        hiveConnectionFactory, hiveInstance);
    hmsFollower.processNotifications(events);

    // invalid event updates notification ID directly
    verify(sentryStore, times(1)).persistLastProcessedNotificationID(inputEventId - 1);

    // next valid event update path, which updates notification ID
    //noinspection unchecked
    verify(sentryStore, times(1)).addAuthzPathsMapping(Mockito.anyString(), Mockito.anyCollection(),
        Mockito.any(Updateable.Update.class));
  }
}
