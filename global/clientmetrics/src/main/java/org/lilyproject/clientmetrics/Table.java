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
package org.lilyproject.clientmetrics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for producing ASCII-tables.
 */
public class Table {
    private List<Column> columns = new ArrayList<Column>();
    private boolean defined = false;
    private String columnFormat;
    private String fullSepLine;
    private String columnSepLine;
    private String crossColumnLineFormat;
    private PrintStream ps;

    public Table(PrintStream ps) {
        this.ps = ps;
    }

    public void addColumn(int width, String title, String type) {
        addColumn(width, title, type, type.equals("s"), type.equals("f") ? 2 : 0);
    }

    public void addColumn(int width, String title, String type, boolean alignLeft, int precision) {
        if (defined) {
            throw new IllegalStateException("Cannot add new columns to table anymore");
        }

        if (width == -1) {
            width = 2 + title.length();
        }

        if (width < 3) {
            width = 3;
        }

        Column column = new Column();
        column.width = width;
        column.title = title;
        column.type = type;
        column.alignLeft = alignLeft;
        column.precision = precision;

        columns.add(column);
    }

    public void finishDefinition() {
        defined = true;

        StringBuilder columnSepLine = new StringBuilder();
        columnSepLine.append('+');
        for (Column column : columns) {
            columnSepLine.append(repeat('-', column.width));
            columnSepLine.append('+');
        }
        this.columnSepLine = columnSepLine.toString();

        int inBetweenWidth = -1;
        for (Column column : columns) {
            inBetweenWidth += column.width + 1;
        }
        this.fullSepLine = "+" + repeat('-', inBetweenWidth) + "+";

        inBetweenWidth -= 2;
        this.crossColumnLineFormat = "| %1$-" + inBetweenWidth + "." + inBetweenWidth + "s |\n";

        StringBuilder columnFormat = new StringBuilder();
        columnFormat.append('|');
        int i = 0;
        for (Column column : columns) {
            i++;
            columnFormat.append("%").append(i).append("$");
            if (column.alignLeft) {
                columnFormat.append("-");
            }
            columnFormat.append(column.width);
            if (column.type.equals("s")) {
                columnFormat.append(".").append(column.width);
            } else if (column.type.equals("f")) {
                columnFormat.append(".").append(column.precision);
            }
            columnFormat.append(column.type);
            columnFormat.append("|");
        }
        columnFormat.append("\n");
        this.columnFormat = columnFormat.toString();
    }

    public void columnSepLine() {
        ps.println(columnSepLine);
    }

    public void fullSepLine() {
        ps.println(fullSepLine);
    }

    public void columns(Object... data) {
        if (data.length != columns.size()) {
            throw new IllegalArgumentException("Number of supplied objects does not match number of defined columns");
        }

        ps.format(columnFormat, data);
    }

    public void crossColumn(String text) {
        ps.format(crossColumnLineFormat, text);
    }

    public void titles() {
        StringBuilder titleFormat = new StringBuilder();
        titleFormat.append("|");
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            int width = column.width - 2;
            titleFormat.append(" %").append(i + 1).append("$-").append(width).append(".").append(width).append("s |");
        }

        Object[] data = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            data[i] = columns.get(i).title;
        }

        titleFormat.append("\n");

        ps.format(titleFormat.toString(), data);
    }

    private String repeat(char ch, int count) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < count; i++) {
            buffer.append(ch);
        }
        return buffer.toString();
    }

    private static class Column {
        int width;
        String title;
        String type;
        int precision;
        boolean alignLeft;
    }
}
