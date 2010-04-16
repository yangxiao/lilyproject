/*
 * Copyright 2010 Outerthought bvba
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
package org.lilycms.repository.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;
import org.lilycms.repository.api.Blob;
import org.lilycms.repository.api.BlobStoreAccess;

public class DFSBlobStoreAccess implements BlobStoreAccess {

    private static final String ID = "DFS";
    
    private final FileSystem fileSystem;

    public DFSBlobStoreAccess(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    public String getId() {
        return ID;
    }
        
    public OutputStream getOutputStream(Blob blob) throws IOException {
        UUID uuid = UUID.randomUUID();
        byte[] blobKey = Bytes.toBytes(uuid.getMostSignificantBits());
        blobKey = Bytes.add(blobKey, Bytes.toBytes(uuid.getLeastSignificantBits()));
        FSDataOutputStream fsDataOutputStream = fileSystem.create(new Path(uuid.toString()));
        return new DFSBlobOutputStream(fsDataOutputStream, blobKey, blob);
    }

    public InputStream getInputStream(byte[] blobKey) throws IOException {
        UUID uuid = new UUID(Bytes.toLong(blobKey), Bytes.toLong(blobKey, Bytes.SIZEOF_LONG));
        FSDataInputStream fsDataInputStream = fileSystem.open(new Path(uuid.toString()));
        return fsDataInputStream;
    }
    
    private class DFSBlobOutputStream extends FilterOutputStream {
        
        private final byte[] blobKey;
        private final Blob blob;
        public DFSBlobOutputStream(OutputStream outputStream, byte[] blobKey, Blob blob) {
            super(outputStream);
            this.blobKey = blobKey;
            this.blob = blob;
        }
        @Override
        public void close() throws IOException {
            super.close();
            blob.setValue(blobKey);
        }
    }
}
