package com.aconex.scrutineer.elasticsearch;

import static org.apache.commons.lang.SystemUtils.getJavaIoTmpDir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import com.aconex.scrutineer.IdAndVersion;
import com.aconex.scrutineer.IdAndVersionStream;
import com.fasterxml.sort.DataReaderFactory;
import com.fasterxml.sort.DataWriterFactory;
import com.fasterxml.sort.SortConfig;
import com.fasterxml.sort.Sorter;
import com.fasterxml.sort.util.NaturalComparator;

public class ElasticSearchIdAndVersionStream implements IdAndVersionStream {

    private static final String ELASTIC_SEARCH_UNSORTED_FILE = "elastic-search-unsorted.dat";

    private static final String ELASTIC_SEARCH_SORTED_FILE = "elastic-search-sorted.dat";
    private static final int DEFAULT_SORT_MEM = 256 * 1024 * 1024;

    private final ElasticSearchDownloader elasticSearchDownloader;
    private final ElasticSearchSorter elasticSearchSorter;
    private final IteratorFactory iteratorFactory;
    private final File unsortedFile;
    private final File sortedFile;

    public ElasticSearchIdAndVersionStream(ElasticSearchDownloader elasticSearchDownloader, ElasticSearchSorter elasticSearchSorter, IteratorFactory iteratorFactory, String workingDirectory) {
        this.elasticSearchDownloader = elasticSearchDownloader;
        this.elasticSearchSorter = elasticSearchSorter;
        this.iteratorFactory = iteratorFactory;
        unsortedFile = new File(workingDirectory, ELASTIC_SEARCH_UNSORTED_FILE);
        sortedFile = new File(workingDirectory, ELASTIC_SEARCH_SORTED_FILE);
    }

    public static ElasticSearchIdAndVersionStream withDefaults(ElasticSearchDownloader elasticSearchDownloader) {
        SortConfig sortConfig = new SortConfig().withMaxMemoryUsage(DEFAULT_SORT_MEM);
        DataReaderFactory<IdAndVersion> dataReaderFactory = new IdAndVersionDataReaderFactory();
        DataWriterFactory<IdAndVersion> dataWriterFactory = new IdAndVersionDataWriterFactory();
        Sorter<IdAndVersion> sorter = new Sorter<IdAndVersion>(sortConfig, dataReaderFactory, dataWriterFactory, new NaturalComparator<IdAndVersion>());

        return new ElasticSearchIdAndVersionStream(elasticSearchDownloader, new ElasticSearchSorter(sorter), new IteratorFactory(), getJavaIoTmpDir().getAbsolutePath());
    }

    @Override
    public void open() {
        elasticSearchDownloader.downloadTo(createUnsortedOutputStream());
        elasticSearchSorter.sort(createUnSortedInputStream(), createSortedOutputStream());
    }

    @Override
    public Iterator<IdAndVersion> iterator() {
        return iteratorFactory.forFile(sortedFile);
    }

    @Override
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void close() {
        unsortedFile.delete();
        sortedFile.delete();
    }

    OutputStream createUnsortedOutputStream() {
        try {
            return new BufferedOutputStream(new FileOutputStream(unsortedFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    InputStream createUnSortedInputStream() {
        try {
            return new BufferedInputStream(new FileInputStream(unsortedFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    OutputStream createSortedOutputStream() {
        try {
            return new BufferedOutputStream(new FileOutputStream(sortedFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
