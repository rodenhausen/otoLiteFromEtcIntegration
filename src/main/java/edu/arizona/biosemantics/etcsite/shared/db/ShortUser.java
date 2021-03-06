package edu.arizona.biosemantics.etcsite.shared.db;

import java.io.Serializable;

public class ShortUser implements Serializable {
	
	private static final long serialVersionUID = 9014388467462637993L;
	private int id;
	private String email;
	private String firstName;
	private String lastName;
	private String affiliation;
	
	public ShortUser() { }
	
	public ShortUser(int id, String email, String firstName, String lastName, String affiliation) {
		this.id = id;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.affiliation = affiliation;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	
	public String getFullName() {
		return firstName + " "  + lastName;
	}
	
	public String getFullNameEmail() {
		return getFullName() + " (" + email + ") ";
	}
	
	public String getFullNameEmailAffiliation() {
		if(affiliation.isEmpty()) 
			return getFullNameEmail();
		return getFullNameEmail() + " at " + affiliation;
	}

	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object object) {
		if(object == null)
			return false;
		if (getClass() != object.getClass()) {
	        return false;
	    }
		ShortUser user = (ShortUser)object;
		if(user.getId()==this.id)
			return true;
		return false;
	}


}

