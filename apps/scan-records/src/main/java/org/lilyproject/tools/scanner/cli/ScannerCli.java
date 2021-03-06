/*
 * Copyright 2012 NGDATA nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilyproject.tools.scanner.cli;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;
import com.ngdata.lily.security.hbase.client.AuthorizationContext;
import org.lilyproject.repository.spi.AuthorizationContextHolder;
import org.lilyproject.util.hbase.RepoAndTableUtil;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.lilyproject.cli.BaseZkCliTool;
import org.lilyproject.cli.OptionUtil;
import org.lilyproject.client.LilyClient;
import org.lilyproject.repository.api.LRepository;
import org.lilyproject.repository.api.LTable;
import org.lilyproject.util.Version;
import org.lilyproject.util.hbase.LilyHBaseSchema.Table;
import org.lilyproject.util.io.Closer;

public class ScannerCli extends BaseZkCliTool {
    private LilyClient lilyClient;
    private Option limitOption;
    private Option countOption;
    private Option printOption;
    private Option configOption;
    private Option startOption;
    private Option stopOption;
    private Option recordTypeOption;
    private Option tableOption;
    private Option repositoryOption;
    private Option rolesOption;

    public static void main(String[] args) {
        new ScannerCli().start(args);
    }

    @Override
    protected String getCmdName() {
        return "lily-scan-records";
    }

    @Override
    protected String getVersion() {
        return Version.readVersion("org.lilyproject", "lily-scan-records");
    }

    @Override
    @SuppressWarnings("static-access")
    public List<Option> getOptions() {
        List<Option> options = super.getOptions();

        limitOption = OptionBuilder
                .withArgName("number")
                .hasArg()
                .withDescription("Limit printing to a number of records")
                .withLongOpt("limit")
                .create("l");
        options.add(limitOption);

        countOption = OptionBuilder
                .withDescription("Count the number of records")
                .withLongOpt("count")
                .create("c");
        options.add(countOption);

        printOption = OptionBuilder
                .withDescription("Print records to the command line")
                .withLongOpt("print")
                .create("p");
        options.add(printOption);

        configOption = OptionBuilder
                .hasArg()
                .withArgName("file")
                .withDescription("Configure the record scanner using a json file")
                .withLongOpt("config")
                .create();
        options.add(configOption);

        startOption = OptionBuilder
                .hasArg()
                .withArgName("id")
                .withDescription("Scan records starting at the record with the given ID")
                .withLongOpt("start")
                .create();
        options.add(startOption);

        stopOption = OptionBuilder
                .hasArg()
                .withArgName("id")
                .withDescription("Scan records stopping at the record with the given ID")
                .withLongOpt("stop")
                .create();
        options.add(stopOption);

        recordTypeOption = OptionBuilder
                .hasArg()
                .withArgName("{namespace}recordTypeName")
                .withDescription("Filter records by record type name")
                .withLongOpt("record-type")
                .create("r");
        options.add(recordTypeOption);

        tableOption = OptionBuilder
                .hasArg()
                .withArgName("table")
                .withDescription("Repository table to scan (defaults to record)")
                .withLongOpt("table")
                .create();
        options.add(tableOption);

        repositoryOption = OptionBuilder
                .hasArg()
                .withArgName("repository")
                .withDescription("Repository name (defaults to 'default')")
                .withLongOpt("repository")
                .create();
        options.add(repositoryOption);

        rolesOption = OptionBuilder
                .withArgName("roles")
                .hasArg()
                .withDescription("Comma-separated list of active user roles (excluding tenant part). Only has "
                        + "effect when the NGDATA hbase-authz coprocessor is installed.")
                .withLongOpt("roles")
                .create();
        options.add(rolesOption);

        return options;
    }

    @Override
    public int run(CommandLine cmd) throws Exception {
        int result = super.run(cmd);
        if (result != 0) {
            return result;
        }

        if (!cmd.hasOption(printOption.getOpt()) && !cmd.hasOption(countOption.getOpt())) {
            printHelp();
            return 0;
        }

        String startId = cmd.hasOption(startOption.getLongOpt()) ? cmd.getOptionValue(startOption.getLongOpt()) : null;
        String stopId = cmd.hasOption(stopOption.getLongOpt()) ? cmd.getOptionValue(stopOption.getLongOpt()) : null;
        String recordTypeFilter = cmd.hasOption(recordTypeOption.getOpt()) ? cmd.getOptionValue(recordTypeOption.getOpt()) : null;
        File configFile = cmd.hasOption(configOption.getLongOpt()) ? new File (cmd.getOptionValue(configOption.getLongOpt())) : null;
        long limit = cmd.hasOption(limitOption.getLongOpt()) ? Long.parseLong(cmd.getOptionValue(limitOption.getLongOpt())) : -1;
        String repositoryName = OptionUtil.getStringOption(cmd, repositoryOption, RepoAndTableUtil.DEFAULT_REPOSITORY);
        String tableName = OptionUtil.getStringOption(cmd, tableOption, Table.RECORD.name);

        if (cmd.hasOption(rolesOption.getLongOpt())) {
            Set<String> roles = new HashSet<String>();
            Splitter splitter = Splitter.on(",").trimResults().omitEmptyStrings();
            for (String role : splitter.split(cmd.getOptionValue(rolesOption.getLongOpt()))) {
                roles.add(role);
            }
            AuthorizationContextHolder.setCurrentContext(new AuthorizationContext("lily-scan-records", repositoryName, roles));
        }

        lilyClient = new LilyClient(zkConnectionString, zkSessionTimeout);
        LRepository repository = lilyClient.getRepository(repositoryName);
        LTable table = repository.getTable(tableName);
        if (cmd.hasOption(countOption.getOpt())) {
            RecordScanTool.count(repository, table, startId, stopId,recordTypeFilter, configFile);
        } else if (cmd.hasOption(printOption.getOpt())) {
            RecordScanTool.print(repository, table, startId, stopId, limit, recordTypeFilter, configFile);
        }

        return 0;
    }

    @Override
    protected void cleanup() {
        Closer.close(lilyClient);
        super.cleanup();
    }

}
