package com.godaddy.vps4.web;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PaginatedResultTest {
    
    public class TestObject{
        public int id;
        public TestObject(int id){
            this.id = id;
        }
    }
    
    private List<TestObject> objList;
    private UriInfo uriInfo;
    
    @Before
    public void setupTest(){
        this.objList = new ArrayList<TestObject>();
        this.objList.add(new TestObject(1));
        this.objList.add(new TestObject(2));
        this.objList.add(new TestObject(3));
        
        this.uriInfo = Mockito.mock(UriInfo.class);
        try{
            Mockito.when(uriInfo.getAbsolutePath()).thenReturn(new URI("http://testHostname/testIdentifier"));
        }catch(URISyntaxException e){
            //do nothing
        }
        
    }
    
    @Test
    public void testConstructor(){
        PaginatedResult<TestObject> pr = new PaginatedResult<>(objList, 3, 5, 10, uriInfo);
        assertEquals(3, pr.pagination.limit);
        assertEquals(10, pr.pagination.total);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=8", pr.pagination.next);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=2", pr.pagination.prev);
        assertEquals(objList, pr.results);
    }
    
    @Test
    public void testAtBeginning(){
        PaginatedResult<TestObject> pr = new PaginatedResult<>(objList, 3, 0, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=0", pr.pagination.prev);
        
        pr = new PaginatedResult<>(objList, 3, 1, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=0", pr.pagination.prev);
        
        pr = new PaginatedResult<>(objList, 3, 3, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=0", pr.pagination.prev);
        
        pr = new PaginatedResult<>(objList, 3, 4, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=1", pr.pagination.prev);
    }
    
    @Test
    public void testAtEnd(){
        PaginatedResult<TestObject> pr = new PaginatedResult<>(objList, 3, 6, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=9", pr.pagination.next);
        
        pr = new PaginatedResult<>(objList, 3, 7, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=7", pr.pagination.next);
        
        pr = new PaginatedResult<>(objList, 3, 10, 10, uriInfo);
        assertEquals("http://testHostname/testIdentifier/?limit=3&offset=10", pr.pagination.next);
    }
    
    

}
