t2eth="contract t2eth {\n"+
"	function t2eth(){\n"+
"		fee=100;\n"+
"		admins[msg.sender]=true;\n"+
"	}\n"+
"       function multiply(uint a) constant returns(uint d) {\n" +
"                  return a * 7;\n" +
"       }\n" +
"	function add_admin(address addr){\n"+
"		if(!admins[msg.sender])\n"+
"			return;\n"+
"		admins[addr]=true;\n"+
"	}\n"+
"	function assign_twitter_handle(bytes32 twitter_handle,address addr){\n"+
"		if(!admins[msg.sender])\n"+
"			return;\n"+
"		twitter_handles[twitter_handle]=addr;\n"+
"	}\n"+
"	function set_fee(uint new_fee){\n"+
"		if(!admins[msg.sender])\n"+
"			return;\n"+
"		if(block.number%10000>4)\n"+
"			return;\n"+
"		fee=new_fee;\n"+
"	}\n"+
"	function check_twitter_handle(bytes32 twitter_handle) returns (address){\n"+
"		uint locked_in_fee=locked_in_fees[msg.sender];\n"+
"		if (locked_in_fee==0){\n"+
"			if (msg.value>=fee){\n"+
"				locked_in_fees[msg.sender]=fee;\n"+
"				return twitter_handles[twitter_handle];				\n"+
"			}\n"+
"		}\n"+
"		else {\n"+
"			if (msg.value>=locked_in_fee)\n"+
"				return twitter_handles[twitter_handle];\n"+
"		}\n"+
"	}\n"+
"	function lock_in_fee(address addr) returns (uint locked_fee){\n"+
"		locked_in_fees[addr]=fee;\n"+
"		return fee;\n"+
"	}\n"+
"	uint fee;\n"+
"	mapping(address=>uint) locked_in_fees;\n"+
"	mapping(address=>bool) admins;\n"+
"	mapping(bytes32=>address) twitter_handles;\n"+
"}\n"
