export interface EmergencyService {
  id: string;
  name: string;
  type: 'Fire Service' | 'Ambulance' | 'Blood Bank' | 'Police';
  district: string;
  division: string;
  phone: string;
  availability: string;
}

export const EMERGENCY_SERVICES: EmergencyService[] = [
  // --- MYMENSINGH DIVISION ---
  { id: 'f1', name: 'উপপরিচালক, ময়মনসিংহ', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901020170', availability: '২৪ ঘণ্টা' },
  { id: 'f2', name: 'সহকারী পরিচালক, ময়মনসিংহ', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901020172', availability: '২৪ ঘণ্টা' },
  { id: 'f3', name: 'ময়মনসিংহ ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024217', availability: '২৪ ঘণ্টা' },
  { id: 'f4', name: 'মুক্তাগাছা ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024227', availability: '২৪ ঘণ্টা' },
  { id: 'f5', name: 'ভালুকা ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024229', availability: '২৪ ঘণ্টা' },
  { id: 'f6', name: 'ত্রিশাল ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024245', availability: '২৪ ঘণ্টা' },
  { id: 'f62', name: 'ঈশ্বরগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024235', availability: '২৪ ঘণ্টা' },
  { id: 'f63', name: 'গফরগাঁও ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024231', availability: '২৪ ঘণ্টা' },
  { id: 'f64', name: 'ফুলপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024241', availability: '২৪ ঘণ্টা' },
  { id: 'f65', name: 'নান্দাইল ফায়ার স্টেশন', type: 'Fire Service', district: 'Mymensingh', division: 'Mymensingh', phone: '01901024249', availability: '২৪ ঘণ্টা' },
  { id: 'f7', name: 'জামালপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Jamalpur', division: 'Mymensingh', phone: '01901024275', availability: '২৪ ঘণ্টা' },
  { id: 'f8', name: 'সরিষাবারি ফায়ার স্টেশন', type: 'Fire Service', district: 'Jamalpur', division: 'Mymensingh', phone: '01901024293', availability: '২৪ ঘণ্টা' },
  { id: 'f66', name: 'ইসলামপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Jamalpur', division: 'Mymensingh', phone: '01901024285', availability: '২৪ ঘণ্টা' },
  { id: 'f67', name: 'মেলান্দহ ফায়ার স্টেশন', type: 'Fire Service', district: 'Jamalpur', division: 'Mymensingh', phone: '01901024291', availability: '২৪ ঘণ্টা' },
  { id: 'f9', name: 'শেরপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Sherpur', division: 'Mymensingh', phone: '01901024299', availability: '২৪ ঘণ্টা' },
  { id: 'f10', name: 'নালিতাবাড়ী ফায়ার স্টেশন', type: 'Fire Service', district: 'Sherpur', division: 'Mymensingh', phone: '01901024305', availability: '২৪ ঘণ্টা' },
  { id: 'f68', name: 'শ্রীবরদী ফায়ার স্টেশন', type: 'Fire Service', district: 'Sherpur', division: 'Mymensingh', phone: '01901024311', availability: '২৪ ঘণ্টা' },
  { id: 'f11', name: 'নেত্রকোণা ফায়ার স্টেশন', type: 'Fire Service', district: 'Netrokona', division: 'Mymensingh', phone: '01901024255', availability: '২৪ ঘণ্টা' },
  { id: 'f12', name: 'মোহনগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Netrokona', division: 'Mymensingh', phone: '01901024269', availability: '২৪ ঘণ্টা' },
  { id: 'f69', name: 'কেন্দুয়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Netrokona', division: 'Mymensingh', phone: '01901024265', availability: '২৪ ঘণ্টা' },
  { id: 'f70', name: 'কলমাকান্দা ফায়ার স্টেশন', type: 'Fire Service', district: 'Netrokona', division: 'Mymensingh', phone: '01901024263', availability: '২৪ ঘণ্টা' },

  // --- RANGPUR DIVISION ---
  { id: 'f13', name: 'রংপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Rangpur', division: 'Rangpur', phone: '01901023231', availability: '২৪ ঘণ্টা' },
  { id: 'f71', name: 'হারাগাছ ফায়ার স্টেশন', type: 'Fire Service', district: 'Rangpur', division: 'Rangpur', phone: '01901023241', availability: '২৪ ঘণ্টা' },
  { id: 'f14', name: 'বদরগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Rangpur', division: 'Rangpur', phone: '01901023235', availability: '২৪ ঘণ্টা' },
  { id: 'f72', name: 'মিঠাপুকুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Rangpur', division: 'Rangpur', phone: '01901023243', availability: '২৪ ঘণ্টা' },
  { id: 'f73', name: 'পীরগঞ্জ ফায়ার স্টেশন (রংপুর)', type: 'Fire Service', district: 'Rangpur', division: 'Rangpur', phone: '01901023247', availability: '২৪ ঘণ্টা' },
  { id: 'f15', name: 'গাইবান্ধা ফায়ার স্টেশন', type: 'Fire Service', district: 'Gaibandha', division: 'Rangpur', phone: '01901023323', availability: '২৪ ঘণ্টা' },
  { id: 'f16', name: 'গোবিন্দগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Gaibandha', division: 'Rangpur', phone: '01901023325', availability: '২৪ ঘণ্টা' },
  { id: 'f74', name: 'সুন্দরগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Gaibandha', division: 'Rangpur', phone: '01901023327', availability: '২৪ ঘণ্টা' },
  { id: 'f17', name: 'কুড়িগ্রাম ফায়ার স্টেশন', type: 'Fire Service', district: 'Kurigram', division: 'Rangpur', phone: '01901023337', availability: '২৪ ঘণ্টা' },
  { id: 'f75', name: 'উলিপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Kurigram', division: 'Rangpur', phone: '01901023341', availability: '২৪ ঘণ্টা' },
  { id: 'f76', name: 'চিলমারী ফায়ার স্টেশন', type: 'Fire Service', district: 'Kurigram', division: 'Rangpur', phone: '01901023345', availability: '২৪ ঘণ্টা' },
  { id: 'f35', name: 'নীলফামারী ফায়ার স্টেশন', type: 'Fire Service', district: 'Nilphamari', division: 'Rangpur', phone: '01901023307', availability: '২৪ ঘণ্টা' },
  { id: 'f77', name: 'সৈয়দপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Nilphamari', division: 'Rangpur', phone: '01901023311', availability: '২৪ ঘণ্টা' },
  { id: 'f78', name: 'জলঢাকা ফায়ার স্টেশন', type: 'Fire Service', district: 'Nilphamari', division: 'Rangpur', phone: '01901023317', availability: '২৪ ঘণ্টা' },
  { id: 'f18', name: 'দিনাজপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Dinajpur', division: 'Rangpur', phone: '01901023251', availability: '২৪ ঘণ্টা' },
  { id: 'f79', name: 'পার্বতীপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Dinajpur', division: 'Rangpur', phone: '01901023259', availability: '২৪ ঘণ্টা' },
  { id: 'f33', name: 'ঠাকুরগাঁও ফায়ার স্টেশন', type: 'Fire Service', district: 'Thakurgaon', division: 'Rangpur', phone: '01901023285', availability: '২৪ ঘণ্টা' },
  { id: 'f34', name: 'পঞ্চগড় ফায়ার স্টেশন', type: 'Fire Service', district: 'Panchagarh', division: 'Rangpur', phone: '01901023297', availability: '২৪ ঘণ্টা' },
  { id: 'f36', name: 'লালমনিরহাট ফায়ার স্টেশন', type: 'Fire Service', district: 'Lalmonirhat', division: 'Rangpur', phone: '01901023359', availability: '২৪ ঘণ্টা' },

  // --- SYLHET DIVISION ---
  { id: 'f19', name: 'সিলেট ফায়ার স্টেশন', type: 'Fire Service', district: 'Sylhet', division: 'Sylhet', phone: '01901023616', availability: '২৪ ঘণ্টা' },
  { id: 'f80', name: 'বিয়ানীবাজার ফায়ার স্টেশন', type: 'Fire Service', district: 'Sylhet', division: 'Sylhet', phone: '01901023634', availability: '২৪ ঘণ্টা' },
  { id: 'f81', name: 'জকিগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Sylhet', division: 'Sylhet', phone: '01901023636', availability: '২৪ ঘণ্টা' },
  { id: 'f20', name: 'সুনামগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Sunamganj', division: 'Sylhet', phone: '01901023698', availability: '২৪ ঘণ্টা' },
  { id: 'f82', name: 'ছাতক ফায়ার স্টেশন', type: 'Fire Service', district: 'Sunamganj', division: 'Sylhet', phone: '01901023700', availability: '২৪ ঘণ্টা' },
  { id: 'f37', name: 'মৌলভীবাজার ফায়ার স্টেশন', type: 'Fire Service', district: 'Maulvibazar', division: 'Sylhet', phone: '01901023658', availability: '২৪ ঘণ্টা' },
  { id: 'f83', name: 'শ্রীমঙ্গল ফায়ার স্টেশন', type: 'Fire Service', district: 'Maulvibazar', division: 'Sylhet', phone: '01901023668', availability: '২৪ ঘণ্টা' },
  { id: 'f38', name: 'হবিগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Habiganj', division: 'Sylhet', phone: '01901023678', availability: '২৪ ঘণ্টা' },
  { id: 'f84', name: 'মাধবপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Habiganj', division: 'Sylhet', phone: '01901023682', availability: '২৪ ঘণ্টা' },

  // --- RAJSHAHI DIVISION ---
  { id: 'f21', name: 'রাজশাহী ফায়ার স্টেশন', type: 'Fire Service', district: 'Rajshahi', division: 'Rajshahi', phone: '01901022227', availability: '২৪ ঘণ্টা' },
  { id: 'f85', name: 'মতিহার ফায়ার স্টেশন', type: 'Fire Service', district: 'Rajshahi', division: 'Rajshahi', phone: '01901022229', availability: '২৪ ঘণ্টা' },
  { id: 'f86', name: 'চারঘাট ফায়ার স্টেশন', type: 'Fire Service', district: 'Rajshahi', division: 'Rajshahi', phone: '01901022237', availability: '২৪ ঘণ্টা' },
  { id: 'f22', name: 'বগুড়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Bogura', division: 'Rajshahi', phone: '01901022381', availability: '২৪ ঘণ্টা' },
  { id: 'f87', name: 'শেরপুর ফায়ার স্টেশন (বগুড়া)', type: 'Fire Service', district: 'Bogura', division: 'Rajshahi', phone: '01901022405', availability: '২৪ ঘণ্টা' },
  { id: 'f88', name: 'সান্তাহার ফায়ার স্টেশন', type: 'Fire Service', district: 'Bogura', division: 'Rajshahi', phone: '01901022411', availability: '২৪ ঘণ্টা' },
  { id: 'f39', name: 'চাঁপাইনবাবগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Chapainawabganj', division: 'Rajshahi', phone: '01901022309', availability: '২৪ ঘণ্টা' },
  { id: 'f89', name: 'শিবগঞ্জ ফায়ার স্টেশন (চাঁপাই)', type: 'Fire Service', district: 'Chapainawabganj', division: 'Rajshahi', phone: '01901022317', availability: '২৪ ঘণ্টা' },
  { id: 'f40', name: 'নওগাঁ ফায়ার স্টেশন', type: 'Fire Service', district: 'Naogaon', division: 'Rajshahi', phone: '01901022255', availability: '২৪ ঘণ্টা' },
  { id: 'f90', name: 'মহাদেবপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Naogaon', division: 'Rajshahi', phone: '01901022259', availability: '২৪ ঘণ্টা' },
  { id: 'f41', name: 'নাটোর ফায়ার স্টেশন', type: 'Fire Service', district: 'Natore', division: 'Rajshahi', phone: '01901022283', availability: '২৪ ঘণ্টা' },
  { id: 'f91', name: 'সিংড়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Natore', division: 'Rajshahi', phone: '01901022295', availability: '২৪ ঘণ্টা' },
  { id: 'f42', name: 'পাবনা ফায়ার স্টেশন', type: 'Fire Service', district: 'Pabna', division: 'Rajshahi', phone: '01901022325', availability: '২৪ ঘণ্টা' },
  { id: 'f92', name: 'ঈশ্বরদী ফায়ার স্টেশন', type: 'Fire Service', district: 'Pabna', division: 'Rajshahi', phone: '01901022335', availability: '২৪ ঘণ্টা' },
  { id: 'f43', name: 'সিরাজগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Sirajganj', division: 'Rajshahi', phone: '01901022353', availability: '২৪ ঘণ্টা' },
  { id: 'f93', name: 'উল্লাপাড়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Sirajganj', division: 'Rajshahi', phone: '01901022375', availability: '২৪ ঘণ্টা' },

  // --- DHAKA DIVISION ---
  { id: 'f23', name: 'ঢাকা বিভাগ (উপপরিচালক)', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020100', availability: '২৪ ঘণ্টা' },
  { id: 'f24', name: 'সিদ্দিকবাজার ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020748', availability: '২৪ ঘণ্টা' },
  { id: 'f25', name: 'উত্তরা ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020782', availability: '২৪ ঘণ্টা' },
  { id: 'f26', name: 'মিরপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020764', availability: '২৪ ঘণ্টা' },
  { id: 'f94', name: 'তেজগাঁও ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020758', availability: '২৪ ঘণ্টা' },
  { id: 'f95', name: 'মোহাম্মদপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020762', availability: '২৪ ঘণ্টা' },
  { id: 'f96', name: 'সাভার ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020820', availability: '২৪ ঘণ্টা' },
  { id: 'f97', name: 'কেরানীগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Dhaka', division: 'Dhaka', phone: '01901020810', availability: '২৪ ঘণ্টা' },
  { id: 'f98', name: 'নারায়ণগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Narayanganj', division: 'Dhaka', phone: '01901020828', availability: '২৪ ঘণ্টা' },
  { id: 'f99', name: 'গাজীপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Gazipur', division: 'Dhaka', phone: '01901020868', availability: '২৪ ঘণ্টা' },
  { id: 'f100', name: 'মুন্সীগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Munshiganj', division: 'Dhaka', phone: '01901020924', availability: '২৪ ঘণ্টা' },
  { id: 'f101', name: 'নরসিংদী ফায়ার স্টেশন', type: 'Fire Service', district: 'Narsingdi', division: 'Dhaka', phone: '01901020902', availability: '২৪ ঘণ্টা' },
  { id: 'f102', name: 'মানিকগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Manikganj', division: 'Dhaka', phone: '01901020946', availability: '২৪ ঘণ্টা' },
  { id: 'f103', name: 'টাঙ্গাইল ফায়ার স্টেশন', type: 'Fire Service', district: 'Tangail', division: 'Dhaka', phone: '01901020962', availability: '২৪ ঘণ্টা' },
  { id: 'f104', name: 'ফরিদপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Faridpur', division: 'Dhaka', phone: '01901020996', availability: '২৪ ঘণ্টা' },
  { id: 'f105', name: 'মাদারীপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Madaripur', division: 'Dhaka', phone: '01901021034', availability: '২৪ ঘণ্টা' },
  { id: 'f106', name: 'গোপালগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Gopalganj', division: 'Dhaka', phone: '01901021046', availability: '২৪ ঘণ্টা' },
  { id: 'f107', name: 'শরীয়তপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Shariatpur', division: 'Dhaka', phone: '01901021058', availability: '২৪ ঘণ্টা' },
  { id: 'f108', name: 'কিশোরগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Kishoreganj', division: 'Dhaka', phone: '01901021074', availability: '২৪ ঘণ্টা' },

  // --- BARISHAL DIVISION ---
  { id: 'f27', name: 'বরিশাল ফায়ার স্টেশন', type: 'Fire Service', district: 'Barishal', division: 'Barishal', phone: '01901023921', availability: '২৪ ঘণ্টা' },
  { id: 'f109', name: 'বাকেরগঞ্জ ফায়ার স্টেশন', type: 'Fire Service', district: 'Barishal', division: 'Barishal', phone: '01901023943', availability: '২৪ ঘণ্টা' },
  { id: 'f28', name: 'ভোলা ফায়ার स्टेशन', type: 'Fire Service', district: 'Bhola', division: 'Barishal', phone: '01901023975', availability: '২৪ ঘণ্টা' },
  { id: 'f44', name: 'পটুয়াখালী ফায়ার স্টেশন', type: 'Fire Service', district: 'Patuakhali', division: 'Barishal', phone: '01901023995', availability: '২৪ ঘণ্টা' },
  { id: 'f45', name: 'পিরোজপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Pirojpur', division: 'Barishal', phone: '01901023959', availability: '২৪ ঘণ্টা' },
  { id: 'f46', name: 'ঝালকাঠি ফায়ার স্টেশন', type: 'Fire Service', district: 'Jhalokati', division: 'Barishal', phone: '01901023951', availability: '২৪ ঘণ্টা' },
  { id: 'f47', name: 'বরগুনা ফায়ার স্টেশন', type: 'Fire Service', district: 'Barguna', division: 'Barishal', phone: '01901024017', availability: '২৪ ঘণ্টা' },

  // --- KHULNA DIVISION ---
  { id: 'f29', name: 'খুলনা ফায়ার স্টেশন', type: 'Fire Service', district: 'Khulna', division: 'Khulna', phone: '01901022730', availability: '২৪ ঘণ্টা' },
  { id: 'f110', name: 'দৌলতপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Khulna', division: 'Khulna', phone: '01901022736', availability: '২৪ ঘণ্টা' },
  { id: 'f30', name: 'যশোর ফায়ার স্টেশন', type: 'Fire Service', district: 'Jashore', division: 'Khulna', phone: '01901022814', availability: '২৪ ঘণ্টা' },
  { id: 'f111', name: 'বেনাপোল ফায়ার স্টেশন', type: 'Fire Service', district: 'Jashore', division: 'Khulna', phone: '01901022830', availability: '২৪ ঘণ্টা' },
  { id: 'f48', name: 'সাতক্ষীরা ফায়ার স্টেশন', type: 'Fire Service', district: 'Satkhira', division: 'Khulna', phone: '01901022796', availability: '২৪ ঘণ্টা' },
  { id: 'f49', name: 'বাগেরহাট ফায়ার স্টেশন', type: 'Fire Service', district: 'Bagerhat', division: 'Khulna', phone: '01901022768', availability: '২৪ ঘণ্টা' },
  { id: 'f50', name: 'কুষ্টিয়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Kushtia', division: 'Khulna', phone: '01901022878', availability: '২৪ ঘণ্টা' },
  { id: 'f51', name: 'ঝিনাইদহ ফায়ার স্টেশন', type: 'Fire Service', district: 'Jhenaidah', division: 'Khulna', phone: '01901022844', availability: '২৪ ঘণ্টা' },
  { id: 'f52', name: 'মাগুরা ফায়ার স্টেশন', type: 'Fire Service', district: 'Magura', division: 'Khulna', phone: '01901022868', availability: '২৪ ঘণ্টা' },
  { id: 'f54', name: 'চুয়াডাঙ্গা ফায়ার স্টেশন', type: 'Fire Service', district: 'Chuadanga', division: 'Khulna', phone: '01901022898', availability: '২৪ ঘণ্টা' },
  { id: 'f55', name: 'মেহেরপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Meherpur', division: 'Khulna', phone: '01901022892', availability: '২৪ ঘণ্টা' },

  // --- CHATTOGRAM DIVISION ---
  { id: 'f31', name: 'চট্টগ্রাম ফায়ার স্টেশন', type: 'Fire Service', district: 'Chattogram', division: 'Chattogram', phone: '01901020110', availability: '২৪ ঘণ্টা' },
  { id: 'f112', name: 'পটিয়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Chattogram', division: 'Chattogram', phone: '01901021601', availability: '২৪ ঘণ্টা' },
  { id: 'f32', name: 'কুমিল্লা ফায়ার স্টেশন', type: 'Fire Service', district: 'Cumilla', division: 'Chattogram', phone: '01901022195', availability: '২৪ ঘণ্টা' },
  { id: 'f113', name: 'চৌদ্দগ্রাম ফায়ার স্টেশন', type: 'Fire Service', district: 'Cumilla', division: 'Chattogram', phone: '01901021711', availability: '২৪ ঘণ্টা' },
  { id: 'f56', name: 'কক্সবাজার ফায়ার স্টেশন', type: 'Fire Service', district: 'Cox\'s Bazar', division: 'Chattogram', phone: '01901021607', availability: '২৪ ঘণ্টা' },
  { id: 'f57', name: 'ব্রাহ্মণবাড়িয়া ফায়ার স্টেশন', type: 'Fire Service', district: 'Brahmanbaria', division: 'Chattogram', phone: '01901021735', availability: '২৪ ঘণ্টা' },
  { id: 'f58', name: 'নোয়াখালী ফায়ার স্টেশন', type: 'Fire Service', district: 'Noakhali', division: 'Chattogram', phone: '01901021775', availability: '২৪ ঘণ্টা' },
  { id: 'f59', name: 'ফেনী ফায়ার স্টেশন', type: 'Fire Service', district: 'Feni', division: 'Chattogram', phone: '01901021823', availability: '২৪ ঘণ্টা' },
  { id: 'f60', name: 'চাঁদপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Chandpur', division: 'Chattogram', phone: '01901021755', availability: '২৪ ঘণ্টা' },
  { id: 'f61', name: 'লক্ষ্মীপুর ফায়ার স্টেশন', type: 'Fire Service', district: 'Lakshmipur', division: 'Chattogram', phone: '01901021805', availability: '২৪ ঘণ্টা' },
  { id: 'f114', name: 'খাগড়াছড়ি ফায়ার স্টেশন', type: 'Fire Service', district: 'Khagrachhari', division: 'Chattogram', phone: '01901021661', availability: '২৪ ঘণ্টা' },
  { id: 'f115', name: 'রাঙ্গামাটি ফায়ার স্টেশন', type: 'Fire Service', district: 'Rangamati', division: 'Chattogram', phone: '01901021637', availability: '২৪ ঘণ্টা' },
  { id: 'f116', name: 'বান্দরবান ফায়ার স্টেশন', type: 'Fire Service', district: 'Bandarban', division: 'Chattogram', phone: '01901021679', availability: '২৪ ঘণ্টা' },
  { id: 'f117', name: 'রাঙ্গামাটি ফায়ার স্টেশন', type: 'Fire Service', district: 'Rangamati', division: 'Chattogram', phone: '01901021637', availability: '২৪ ঘণ্টা' },
  { id: 'f118', name: 'খাগড়াছড়ি ফায়ার স্টেশন', type: 'Fire Service', district: 'Khagrachhari', division: 'Chattogram', phone: '01901021661', availability: '২৪ ঘণ্টা' },

  // --- ADDITIONAL AMBULANCE & BLOOD BANKS ---
  { id: 'a1', name: 'আঞ্জুমান মফিদুল ইসলাম (ঢাকা)', type: 'Ambulance', district: 'Dhaka', division: 'Dhaka', phone: '02-9333301', availability: '২৪ ঘণ্টা' },
  { id: 'a2', name: 'ফায়ার সার্ভিস অ্যাম্বুলেন্স (ঢাকা)', type: 'Ambulance', district: 'Dhaka', division: 'Dhaka', phone: '02-9555555', availability: '২৪ ঘণ্টা' },
  { id: 'a3', name: 'মমতা অ্যাম্বুলেন্স সার্ভিস (চট্টগ্রাম)', type: 'Ambulance', district: 'Chattogram', division: 'Chattogram', phone: '01819315555', availability: '২৪ ঘণ্টা' },
  { id: 'b1', name: 'রেড ক্রিসেন্ট ব্লাড ব্যাংক (ঢাকা)', type: 'Blood Bank', district: 'Dhaka', division: 'Dhaka', phone: '02-9112233', availability: '২৪ ঘণ্টা' },
  { id: 'b2', name: 'সন্ধানী ব্লাড ব্যাংক (রংপুর)', type: 'Blood Bank', district: 'Rangpur', division: 'Rangpur', phone: '0521-62325', availability: '২৪ ঘণ্টা' },
  { id: 'b3', name: 'কোয়ান্টাম ব্লাড ব্যাংক (ঢাকা)', type: 'Blood Bank', district: 'Dhaka', division: 'Dhaka', phone: '01714010869', availability: '২৪ ঘণ্টা' },

  // --- POLICE STATIONS ---
  { id: 'p1', name: 'ডিএমপি কন্ট্রোল রুম', type: 'Police', district: 'Dhaka', division: 'Dhaka', phone: '02-999', availability: '২৪ ঘণ্টা' },
  { id: 'p2', name: 'সিএমপি কন্ট্রোল রুম', type: 'Police', district: 'Chattogram', division: 'Chattogram', phone: '031-638066', availability: '২৪ ঘণ্টা' },
  { id: 'p3', name: 'রংপুর মহানগর পুলিশ কন্ট্রোল রুম', type: 'Police', district: 'Rangpur', division: 'Rangpur', phone: '01320-130000', availability: '২৪ ঘণ্টা' },
  
  // --- NATIONAL EMERGENCY ---
  { id: 'n1', name: 'ন্যাশনাল ইমার্জেন্সি (পুলিশ, ফায়ার, অ্যাম্বুলেন্স)', type: 'Police', district: 'সারাদেশ', division: 'সারাদেশ', phone: '999', availability: '২৪ ঘণ্টা' },
];
